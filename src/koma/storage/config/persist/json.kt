package koma.storage.config.persist

import com.squareup.moshi.Moshi
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException

fun<T> load_json(file: File?, jclass: Class<T>, adapters: List<Any> = listOf()): T? {
    file?: return null
    var mbuilder = Moshi.Builder()
    for (adapt in adapters) {
        mbuilder = mbuilder.add(adapt)
    }
    val jsonAdapter = mbuilder
            .build()
            .adapter(jclass)
    val saved= try {
        jsonAdapter.fromJson(file.readText())
    } catch (e: FileNotFoundException) {
        null
    } catch (e: IOException) {
        e.printStackTrace()
        null
    }

    return saved
}

fun<T: Any> save_json(file: File?, data: T, jclass: Class<T>, adapters: List<Any> = listOf()) {
    file?:return
    var moshi = Moshi.Builder()
    for (adapater in adapters) {
        moshi = moshi.add(adapater)
    }
    val jsonAdapter = moshi.build().adapter(jclass).indent("    ")
    val json = try {
        jsonAdapter.toJson(data)
    } catch (e: ClassCastException) {
        e.printStackTrace()
        return
    }
    try {
        file.writeText(json)
    } catch (e: IOException) {
    }
}
