package koma.network.media

import com.github.kittinunf.result.Result
import com.github.kittinunf.result.flatMap
import com.github.kittinunf.result.map
import koma.network.client.okhttp.AppHttpClient
import koma.util.coroutine.adapter.okhttp.await
import koma.util.coroutine.adapter.okhttp.extract
import okhttp3.CacheControl
import okhttp3.HttpUrl
import okhttp3.Request
import okhttp3.ResponseBody
import java.util.concurrent.TimeUnit

suspend fun getResponse(url: HttpUrl): Result<ResponseBody, Exception> {
    val req = Request.Builder().url(url).build()
    val httpres = AppHttpClient.client.newCall(req).await()
    return httpres.flatMap { res -> res.extract() }
}

suspend fun downloadMedia(url: HttpUrl, forceCacheDays: Int?=null): Result<ByteArray, Exception> {
    val reqb = Request.Builder().url(url)
    val req = (if (forceCacheDays != null) reqb
            .cacheControl(CacheControl.Builder()
                    .maxStale(forceCacheDays, TimeUnit.DAYS)
                    .build())
    else reqb)
            .build()
    val bs = getHttpBytes(req)
    return bs
}

private suspend fun getHttpBytes(req: Request): Result<ByteArray, Exception> {
    val hr = AppHttpClient.client.newCall(req).await()
    return hr.flatMap { it.extract() }.map { it.bytes() }
}

