package link.continuum.database.models

import io.requery.Column
import io.requery.Entity
import io.requery.Key
import io.requery.Persistable
import io.requery.kotlin.eq
import io.requery.sql.KotlinEntityDataStore
import mu.KotlinLogging
import java.io.*
import kotlin.reflect.KProperty

val _logger = KotlinLogging.logger {}

@Entity
interface DbKeyValue: Persistable {
    @get:Key
    var key: String

    @get:Column(nullable = false, length = Int.MAX_VALUE)
    var bytes: ByteArray
}

class KeyValueEntry<T>(
        private val _load: (name: String)-> T?,
        private val _store: (name: String, value: T)->Unit,
        private val default: T
) {
    private var loaded = false
    private var cache: T? = null

    operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
        if (!loaded) {
            _logger.debug { "loading ${property.name}" }
            loaded = true
            cache = _load(property.name)
        }
        return cache ?: default
    }

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T?) {
        value?.let {
            cache = it
            _logger.debug { "storing ${property.name}=$it"}
            _store(property.name, it)
        }
    }
}


inline fun<reified T: Serializable> dbkv(data: KotlinEntityDataStore<Persistable>, default: T): KeyValueEntry<T> {
    return KeyValueEntry<T>(
            default = default,
            _load = { name ->
                val c = DbKeyValue::key.eq(name)
                val b = data.select(DbKeyValue::class).where(c).get().firstOrNull()
                b ?:return@KeyValueEntry null
                deserialize<T>(b.bytes)
            },
            _store = { name, value ->
                val b = serialize(value)
                if (b == null) {
                    _logger.error { "can't serialize $value" }
                    return@KeyValueEntry
                }
                val entity = DbKeyValueEntity()
                entity.key = name
                entity.bytes = b
                data.upsert(entity)
            }
    )
}

inline fun<reified T> deserialize(array: ByteArray): T? {
    val ins = ByteArrayInputStream(array)
    val ois = ObjectInputStream(ins)
    val o = try {
        ois.readObject()
    } catch (e: Exception) {
        _logger.error { "can't readObject to deserialize, $e" }
    }
    if (o is T) {
        return o
    }
    _logger.error { "deserialized $o is not ${T::class.java.name}" }
    return null
}

fun<T> serialize(value: T): ByteArray? {
    val ins = ByteArrayOutputStream()
    val ois = ObjectOutputStream(ins)
    val o = try {
        ois.writeObject(value)
    } catch (e: Exception) {
        _logger.error { "can't serialize $value, $e" }
    } finally {
        ois.close()
    }
    return ins.toByteArray()
}
