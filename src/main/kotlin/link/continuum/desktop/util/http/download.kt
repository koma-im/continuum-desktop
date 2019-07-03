package link.continuum.desktop.util.http

import koma.Failure
import koma.util.*
import koma.util.coroutine.adapter.okhttp.await
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import link.continuum.desktop.gui.switchGetDeferredOption
import link.continuum.desktop.util.*
import mu.KotlinLogging
import okhttp3.*
import java.util.*
import java.util.concurrent.TimeUnit

private val logger = KotlinLogging.logger {}
private typealias Option<T> = Optional<T>

suspend fun downloadHttp(
        url: HttpUrl, client: OkHttpClient, maxStale: Int? = null
): KResult<ByteArray, Failure> {
    val req = Request.Builder().url(url).let {
        if (maxStale != null) {
            it.cacheControl(CacheControl
                    .Builder()
                    .maxStale(maxStale, TimeUnit.SECONDS)
                    .build())
        } else { it }
    }.build()
    val res = client.newCall(req).await() getOr  { return Err(it)}
    val b = res.body()
    if (!res.isSuccessful || b == null) {
        return fmtErr { "failed to get response body for $url" }
    }
    return Ok(b.use { it.bytes() })
}

/**
 * given a channel of URLs, get the latest download
 */
fun CoroutineScope.urlChannelDownload(client: OkHttpClient
): Pair<SendChannel<Option<HttpUrl>>, ReceiveChannel<Option<ByteArray>>> {
    val url = Channel<Option<HttpUrl>>(Channel.CONFLATED)
    val bytes = Channel<Option<ByteArray>>(Channel.CONFLATED)
    switchGetDeferredOption(url, { u ->
        deferredDownload(u, client)
    }, bytes)
    return url to bytes
}

fun CoroutineScope.deferredDownload(url: HttpUrl, client: OkHttpClient) = async {
    downloadHttp(url, client).fold({
        Some(it)
    }, {
        logger.warn { "deferredDownload of $url error $it" }
        None<ByteArray>()
    })
}
