package koma.util.file

import okhttp3.MediaType
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.net.URLConnection

fun File.guessFileMime(): String? {
    val fis = FileInputStream(this)
    val bis = BufferedInputStream(fis)
    val mime = URLConnection.guessContentTypeFromStream(bis)
    return mime
}

fun File.guessMediaType(): MediaType? {
    return this.guessFileMime()?.let { MediaType.parse(it) }
}

