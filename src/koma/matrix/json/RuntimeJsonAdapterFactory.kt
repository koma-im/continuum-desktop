package koma.matrix.json

import com.squareup.moshi.*
import java.io.IOException
import java.lang.reflect.Type
import java.util.*

/**
 * https://github.com/square/moshi/commit/924a2490beabc7fe511d75fe9ed34b743e9265e4
 */
class RuntimeJsonAdapterFactory<T: Any> constructor(
        private val baseType: Class<T>,
        private val labelKey: String,
        private val defaultType: Class<out T>
) : JsonAdapter.Factory {
    private val subtypeToLabel = LinkedHashMap<Class<out T>, String>()

    fun registerSubtype(subtype: Class<out T>, label: String): RuntimeJsonAdapterFactory<T> {
        if (!baseType.isAssignableFrom(subtype)) {
            throw IllegalArgumentException(subtype.toString() + " must be a " + baseType)
        }
        subtypeToLabel.put(subtype, label)
        return this
    }

    fun registerAllSubtypes(subtypes: Map<Class<out T>, String>): RuntimeJsonAdapterFactory<T> {
        subtypeToLabel.putAll(subtypes)
        return this
    }

    override fun create(type: Type, annotations: Set<Annotation>, moshi: Moshi): JsonAdapter<T>? {
        if (annotations.isNotEmpty() || Types.getRawType(type) != baseType) {
            return null
        }
        val subtypeToLabel = LinkedHashMap(this.subtypeToLabel)
        val size = subtypeToLabel.size
        val labelToDelegate = LinkedHashMap<String, JsonAdapter<T>>(size)
        val subtypeToDelegate = LinkedHashMap<Class<out T>, JsonAdapter<T>>(size)
        for ((key, value) in subtypeToLabel) {
            val delegate = moshi.adapter<T>(key, annotations)
            labelToDelegate.put(value, delegate)
            subtypeToDelegate.put(key, delegate)
        }
        val delegateDefault = moshi.adapter<T>(defaultType, annotations)
        val toJsonDelegate = moshi.adapter<Map<String, Any>>(Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java))
        return RuntimeJsonAdapter<T>(labelKey, labelToDelegate, subtypeToDelegate, subtypeToLabel,
                toJsonDelegate, delegateDefault)
    }

    private class RuntimeJsonAdapter<T: Any> internal constructor(private val labelKey: String,
                                                          private val labelToDelegate: Map<String, JsonAdapter<T>>,
                                                          private val subtypeToDelegate: Map<Class<out T>, JsonAdapter<T>>,
                                                          private val subtypeToLabel: Map<Class<out T>, String>,
                                                          private val toJsonDelegate: JsonAdapter<Map<String, Any>>,
                                                          private val defaultDelegate: JsonAdapter<T>) : JsonAdapter<T>() {

        @Throws(IOException::class)
        override fun fromJson(reader: JsonReader): T? {
            val raw = reader.readJsonValue()
            raw ?: throw JsonDataException("Value must be a JSON object but had a value of null")
            if (raw !is Map<*, *>) {
                throw JsonDataException(
                        "Value must be a JSON object but had a value of " + raw + " of type " + raw.javaClass)
            }
            val value = raw as MutableMap<String, Any>// This is a JSON object.
            if (value.isEmpty()) return null
            val label = value.remove(labelKey) ?: throw JsonDataException("Missing label for " + labelKey + " in " + value.toString())
            if (label !is String) {
                throw JsonDataException("Label for "
                        + labelKey
                        + " must be a string but had a value of "
                        + label
                        + " of type "
                        + label.javaClass)
            }
            var delegate: JsonAdapter<T>? = labelToDelegate[label]
            if (delegate == null) {
                System.err.println("using default delegate for label " + label + " value " + value.toString())
                delegate = defaultDelegate
            }
            return delegate.fromJsonValue(value)
        }

        @Throws(IOException::class)
        override fun toJson(writer: JsonWriter, value: T?) {
            if (value == null) throw JsonDataException("can't encode null as $labelKey")
            val subtype = value.javaClass
            val delegate = subtypeToDelegate[subtype] ?: throw JsonDataException("Type not registered: " + subtype)// The delegate is a JsonAdapter<subtype>.
            val jsonValue = delegate.toJsonValue(value) as MutableMap<String, Any>// This is a JSON object.
            val sublabel = subtypeToLabel[subtype]!!
            val existingLabel = jsonValue.put(labelKey, sublabel)
            if (existingLabel != null) {
                throw JsonDataException(
                        "Label field $labelKey already defined as $existingLabel")
            }
            toJsonDelegate.toJson(writer, jsonValue)
        }
    }
}
