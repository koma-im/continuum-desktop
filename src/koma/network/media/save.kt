package koma.network.media

import koma.Koma
import koma.util.result.ok
import okhttp3.HttpUrl
import okio.Okio
import java.io.File

suspend fun Koma.saveUrlToFile(url: HttpUrl, file: File): Boolean {
    val body = getResponse(url).ok()
    body?: return false

    val sink = Okio.sink(file)
    val bs = Okio.buffer(sink)
    bs.writeAll(body.source())
    bs.close()
    body.close()
    return true
}
