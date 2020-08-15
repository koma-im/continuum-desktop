package link.continuum.desktop.util.http

import koma.Failure
import koma.Server
import koma.network.media.MHUrl
import koma.util.*
import koma.util.coroutine.adapter.okhttp.await
import link.continuum.desktop.util.*
import mu.KotlinLogging
import okhttp3.*
import java.util.*
import java.util.concurrent.TimeUnit

private val logger = KotlinLogging.logger {}
private typealias Option<T> = Optional<T>
typealias MediaServer = Server

suspend fun downloadHttp(
        url: HttpUrl, client: OkHttpClient, maxStale: Int? = null
): KResult<ByteArray, Failure> {
    val req = Request.Builder().url(url).given(maxStale) {
        cacheControl(CacheControl
                    .Builder()
                    .maxStale(it, TimeUnit.SECONDS)
                    .build())
    }.build()
    val r = client.newCall(req).await()
    if (r.isFailure) return Err(r.failureOrThrow())
    val res = r.getOrThrow()
    val b = res.body
    if (!res.isSuccessful || b == null) {
        return fmtErr { "failed to get response body for $url" }
    }
    return Ok(b.use { it.bytes() })
}