package koma.matrix.json

import com.squareup.moshi.*
import java.io.IOException
import java.lang.reflect.Constructor
import java.lang.reflect.Type

abstract class NewTypeString(val str: String) {
    final override fun toString(): String = str
    abstract override fun equals(other: Any?): Boolean
    abstract override fun hashCode(): Int
}

class NewTypeStringAdapterFactory: JsonAdapter.Factory {
    private val newStringClass = NewTypeString::class.java

    override fun create(type: Type, annotations: Set<Annotation>, moshi: Moshi): JsonAdapter<NewTypeString>? {
        val rawType = Types.getRawType(type)
        if (annotations.isNotEmpty() || !newStringClass.isAssignableFrom(rawType)) {
            return null
        }
        val toJson = moshi.adapter(String::class.java)
        @Suppress("UNCHECKED_CAST")
        val cons = rawType.getConstructor(String::class.java) as Constructor<NewTypeString>
        return NewTypeStringAdapter(cons, toJson)
    }

    private class NewTypeStringAdapter internal constructor(
            private val constructor: Constructor<NewTypeString>,
            private val toJsonDelegate: JsonAdapter<String>
    ) : JsonAdapter<NewTypeString>() {
        @Throws(IOException::class)
        override fun fromJson(reader: JsonReader): NewTypeString? {
            val raw = reader.readJsonValue()
            raw ?: throw JsonDataException("Value must be a JSON object but had a value of null")
            if (raw !is String)
                throw JsonDataException("Value must be a string but had a value of " + raw + " type of " + raw.javaClass)
            val value = raw
            if (value.isEmpty()) return null
            return constructor.newInstance(value)
        }

        @Throws(IOException::class)
        override fun toJson(writer: JsonWriter, value: NewTypeString?) {
            if (value == null) throw JsonDataException("can't encode null as NewTypeString")
            toJsonDelegate.toJson(writer, value.str)
        }
    }
}
