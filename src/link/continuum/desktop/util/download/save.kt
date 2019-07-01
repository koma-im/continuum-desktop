package link.continuum.desktop.util.download

import koma.Koma
import koma.network.media.getResponse
import koma.util.getOr
import okhttp3.HttpUrl
import okio.Okio
import java.io.File

suspend fun Koma.saveUrlToFile(url: HttpUrl, file: File): Boolean {
    val body = getResponse(url) getOr { return false }
    val sink = Okio.sink(file)
    val bs = Okio.buffer(sink)
    bs.writeAll(body.source())
    bs.close()
    body.close()
    return true
}
