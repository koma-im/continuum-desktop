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
import java.io.File

fun CoroutineScope.saveUrlToFile(url: HttpUrl, file: File, httpClient: OkHttpClient) = async(Dispatchers.IO){
    val b = getResponse(httpClient, url)
    if (b.isFailure) return@async false
    val body = b.getOrThrow()
    val sink = Okio.sink(file)
    val bs = Okio.buffer(sink)
    bs.writeAll(body.source())
    bs.close()
    body.close()
    return@async true
}
