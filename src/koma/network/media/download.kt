package koma.network.media

import koma.network.client.okhttp.AppHttpClient
import koma.util.coroutine.adapter.okhttp.awaitResult
import koma.util.result.http.HttpResult
import koma.util.result.http.body.BodyResult
import koma.util.result.http.body.bodyResult
import okhttp3.HttpUrl
import okhttp3.Request
import okhttp3.ResponseBody

suspend fun getResponse(url: HttpUrl): ResponseBody? {
    val req = Request.Builder().url(url).build()
    val hr = AppHttpClient.client.newCall(req).awaitResult()
    if (hr !is HttpResult.Ok) return null
    val br = hr.response.bodyResult()
    if (br is BodyResult.Ok) return br.body
    return null
}

suspend fun downloadMedia(url: HttpUrl): ByteArray? {
    val req = Request.Builder().url(url).build()
    val hr = AppHttpClient.client.newCall(req).awaitResult()
    return when (hr) {
        is HttpResult.Ok -> {
            val br = hr.response.bodyResult()
            when (br) {
                is BodyResult.Ok -> br.body.bytes()
                is BodyResult.Error -> {
                    System.err.println("http error getting $url: ${br.error.message}")
                    br.error.response.close()
                    null
                }
            }
        }
        is HttpResult.Exception -> {
            System.err.println("exception getting $url, with error ${hr.exception.message}")
            null
        }
    }
}

