package koma.network.media

import okhttp3.HttpUrl
import okio.Okio
import java.io.File

suspend fun saveUrlToFile(url: HttpUrl, file: File): Boolean {
    val body = getResponse(url)
    body?: return false

    val sink = Okio.sink(file)
    val bs = Okio.buffer(sink)
    bs.writeAll(body.source())
    bs.close()
    body.close()
    return true
}
