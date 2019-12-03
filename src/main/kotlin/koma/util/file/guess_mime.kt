package koma.util.file

import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.net.URLConnection
import koma.util.ContentType

fun File.guessFileMime(): String? {
    val mime = FileInputStream(this).use {
        BufferedInputStream(it).use {
            URLConnection.guessContentTypeFromStream(it)
        }
    }
    return mime
}

fun File.guessMediaType(): ContentType? {
    return this.guessFileMime()?.let { ContentType.parse(it) }
}

