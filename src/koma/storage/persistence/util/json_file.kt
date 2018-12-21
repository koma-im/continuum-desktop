package koma.storage.persistence.util

import koma.matrix.json.MoshiInstance
import koma.storage.persistence.settings.encoding.ProxyAdapter
import mu.KotlinLogging
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException

private val logger = KotlinLogging.logger {}

private object json {
    val moshi = MoshiInstance.moshiBuilder.add(ProxyAdapter()).build()
}

fun<T: Any> load_json(file: File?, jclass: Class<T>): T? {
    file?: return null
    val saved= try {
        val jsonAdapter = json.moshi.adapter(jclass)
        jsonAdapter.fromJson(file.readText())
    } catch (_e: FileNotFoundException) {
        logger.warn { "File not found: $file" }
        null
    } catch (e: IOException) {
        logger.warn { "Failed to load $file: $e" }
        null
    }

    return saved
}

fun<T: Any> save_json(file: File?, data: T, jclass: Class<T>) {
    file?:return
    val jsonAdapter = json.moshi
            .adapter(jclass)
            .indent("    ")
    val json = try {
        jsonAdapter.toJson(data)
    } catch (_e: Exception) {
        logger.warn { "Failed to encode json to be saved in $file" }
        return
    }
    try {
        file.writeText(json)
    } catch (e: IOException) {
        logger.warn { "Failed to save $file: $e" }
    }
}
