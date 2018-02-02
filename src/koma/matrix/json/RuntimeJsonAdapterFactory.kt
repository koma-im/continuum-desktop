package koma.matrix.json

import com.squareup.moshi.*
import java.io.IOException
import java.lang.reflect.Type
import java.util.*

/**
 * https://github.com/square/moshi/commit/924a2490beabc7fe511d75fe9ed34b743e9265e4
 */
class RuntimeJsonAdapterFactory(private val baseType: Class<*>, private val labelKey: String) : JsonAdapter.Factory {
    private val subtypeToLabel = LinkedHashMap<Class<*>, String>()
    private var defaultType: Class<*>? = null

    fun registerDefaultType(defaultType: Class<*>): RuntimeJsonAdapterFactory {
        if (!baseType.isAssignableFrom(defaultType)) {
            throw IllegalArgumentException(defaultType.toString() + " must be a " + baseType)
        }
        this.defaultType = defaultType
        return this
    }

    fun registerSubtype(subtype: Class<*>?, label: String?): RuntimeJsonAdapterFactory {
        if (subtype == null) {
            throw NullPointerException("subtype == null")
        }
        if (label == null) {
            throw NullPointerException("label == null")
        }
        if (!baseType.isAssignableFrom(subtype)) {
            throw IllegalArgumentException(subtype.toString() + " must be a " + baseType)
        }
        subtypeToLabel.put(subtype, label)
        return this
    }

    override fun create(type: Type, annotations: Set<Annotation>, moshi: Moshi): JsonAdapter<*>? {
        if (!annotations.isEmpty() || Types.getRawType(type) != baseType) {
            return null
        }
        val subtypeToLabel = LinkedHashMap(this.subtypeToLabel)
        val size = subtypeToLabel.size
        val labelToDelegate = LinkedHashMap<String, JsonAdapter<*>>(size)
        val subtypeToDelegate = LinkedHashMap<Class<*>, JsonAdapter<*>>(size)
        for ((key, value) in subtypeToLabel) {
            val delegate = moshi.adapter<Any>(key, annotations)
            labelToDelegate.put(value, delegate)
            subtypeToDelegate.put(key, delegate)
        }
        val delegateDefault: JsonAdapter<*>?
        if (defaultType != null) {
            delegateDefault = moshi.adapter<Any>(defaultType, annotations)
        } else {
            delegateDefault = null
        }
        val toJsonDelegate = moshi.adapter<Map<String, Any>>(Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java))
        return RuntimeJsonAdapter(labelKey, labelToDelegate, subtypeToDelegate, subtypeToLabel,
                toJsonDelegate, delegateDefault)
    }

    private class RuntimeJsonAdapter internal constructor(private val labelKey: String,
                                                          private val labelToDelegate: Map<String, JsonAdapter<*>>,
                                                          private val subtypeToDelegate: Map<Class<*>, JsonAdapter<*>>,
                                                          private val subtypeToLabel: Map<Class<*>, String>,
                                                          private val toJsonDelegate: JsonAdapter<Map<String, Any>>,
                                                          private val defaultDelegate: JsonAdapter<*>?) : JsonAdapter<Any>() {

        @Throws(IOException::class)
        override fun fromJson(reader: JsonReader): Any? {
            val raw = reader.readJsonValue()
            if (raw !is Map<*, *>) {
                throw JsonDataException(
                        "Value must be a JSON object but had a value of " + raw + " of type " + raw!!.javaClass)
            }
            val value = raw as MutableMap<String, Any>?// This is a JSON object.
            if (value!!.isEmpty()) return null
            val label = value.remove(labelKey) ?: throw JsonDataException("Missing label for " + labelKey + " in " + value.toString())
            if (label !is String) {
                throw JsonDataException("Label for "
                        + labelKey
                        + " must be a string but had a value of "
                        + label
                        + " of type "
                        + label!!.javaClass)
            }
            var delegate: JsonAdapter<*>? = labelToDelegate[label]
            if (delegate == null) {
                if (defaultDelegate != null) {
                    System.err.println("using default delegate for label " + label + " value " + value.toString())
                    delegate = defaultDelegate
                } else {
                    System.err.println("no fallback delegate found for label " + label + " value " + value.toString())
                    throw JsonDataException("Type not registered for label: " + label!!)
                }
            }
            return delegate.fromJsonValue(value)
        }

        @Throws(IOException::class)
        override fun toJson(writer: JsonWriter, value: Any?) {
            if (value == null) throw JsonDataException("can't encode null as $labelKey")
            val subtype = value.javaClass
            val delegate = subtypeToDelegate[subtype] as JsonAdapter<Any>? ?: throw JsonDataException("Type not registered: " + subtype)// The delegate is a JsonAdapter<subtype>.
            val jsonValue = delegate.toJsonValue(value) as MutableMap<String, Any>?// This is a JSON object.
            val sublabel = subtypeToLabel[subtype]!!
            val existingLabel = jsonValue!!.put(labelKey, sublabel)
            if (existingLabel != null) {
                throw JsonDataException(
                        "Label field $labelKey already defined as $existingLabel")
            }
            toJsonDelegate.toJson(writer, jsonValue)
        }
    }
}
