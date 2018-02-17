package koma.network.media

import koma.network.client.okhttp.AppHttpClient
import koma.util.coroutine.adapter.okhttp.awaitResult
import koma.util.result.http.HttpResult
import koma.util.result.http.body.BodyResult
import koma.util.result.http.body.bodyResult
import okhttp3.CacheControl
import okhttp3.HttpUrl
import okhttp3.Request
import okhttp3.ResponseBody
import java.util.concurrent.TimeUnit

suspend fun getResponse(url: HttpUrl): ResponseBody? {
    val req = Request.Builder().url(url).build()
    val hr = AppHttpClient.client.newCall(req).awaitResult()
    if (hr !is HttpResult.Ok) return null
    val br = hr.response.bodyResult()
    if (br is BodyResult.Ok) return br.body
    return null
}

suspend fun downloadMedia(url: HttpUrl, forceCacheDays: Int?=null): ByteArray? {
    val req = Request.Builder().url(url).build()
    if (forceCacheDays != null) {
        val cacheReq = Request.Builder().url(url)
                .cacheControl(CacheControl.Builder()
                        .onlyIfCached()
                        .maxStale(forceCacheDays, TimeUnit.DAYS)
                        .build())
                .build()
        val cacheBytes = getHttpBytes(cacheReq)
        return cacheBytes?: getHttpBytes(req)
    } else {
       return getHttpBytes(req)
    }

}

private suspend fun getHttpBytes(req: Request): ByteArray? {
    val hr = AppHttpClient.client.newCall(req).awaitResult()
    return when (hr) {
        is HttpResult.Ok -> {
            val br = hr.response.bodyResult()
            when (br) {
                is BodyResult.Ok -> br.body.bytes()
                is BodyResult.Error -> {
                    System.err.println("http error getting ${req.url()}: ${br.error.message}")
                    br.error.response.close()
                    null
                }
            }
        }
        is HttpResult.Exception -> {
            System.err.println("exception getting ${req.url()}, with error ${hr.exception.message}")
            null
        }
    }
}

