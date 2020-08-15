package link.continuum.desktop.util.download

import koma.network.media.getResponse
import koma.util.getOrThrow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okio.Okio
import okio.buffer
import okio.sink
import java.io.File

fun CoroutineScope.saveUrlToFile(url: HttpUrl, file: File, httpClient: OkHttpClient) = async(Dispatchers.IO){
    val b = getResponse(httpClient, url)
    if (b.isFailure) return@async false
    val body = b.getOrThrow()
    val sink = file.sink()
    val bs = sink.buffer()
    bs.writeAll(body.source())
    bs.close()
    body.close()
    return@async true
}
