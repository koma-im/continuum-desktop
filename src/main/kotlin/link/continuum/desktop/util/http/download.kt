package link.continuum.desktop.util.http

import com.github.kittinunf.result.flatMap
import com.github.kittinunf.result.map
import koma.util.coroutine.adapter.okhttp.await
import link.continuum.desktop.util.ErrorMsg
import link.continuum.desktop.util.KResult
import link.continuum.desktop.util.Ok
import link.continuum.desktop.util.fmtErr
import okhttp3.*
import java.util.concurrent.TimeUnit

suspend fun downloadHttp(
        url: HttpUrl, client: OkHttpClient, maxStale: Int? = null
): KResult<ByteArray, Exception> {
    val req = Request.Builder().url(url).let {
        if (maxStale != null) {
            it.cacheControl(CacheControl
                    .Builder()
                    .maxStale(maxStale, TimeUnit.SECONDS)
                    .build())
        } else { it }
    }.build()
    return client.newCall(req).await().flatMap { res ->
        val b = res.body()
        if (res.isSuccessful && b !=null) {
            Ok<ResponseBody, ErrorMsg>(b)
        } else {
            fmtErr { "failed to get response body for $url" }
        }
    }.map { it.use { it.bytes() } }
}
