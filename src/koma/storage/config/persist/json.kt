package koma.storage.config.persist

import com.squareup.moshi.Moshi
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException

fun<T> load_json(file: File?, jclass: Class<T>): T? {
    file?: return null
    val jsonAdapter = Moshi.Builder()
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

fun<T: Any> save_json(file: File?, data: T, jclass: Class<T>) {
    file?:return
    val moshi = Moshi.Builder()
            .build()
    val jsonAdapter = moshi.adapter(jclass).indent("    ")
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
