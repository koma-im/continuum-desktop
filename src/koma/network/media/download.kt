package koma.network.media

import javafx.scene.image.Image
import koma.network.client.okhttp.AppHttpClient
import koma.network.matrix.media.makeAnyUrlHttp
import okhttp3.HttpUrl
import okhttp3.Request
import okhttp3.ResponseBody

fun downloadImageUri(uri: String): Image? {
    return makeAnyUrlHttp(uri)?.let { downloadImageHttp(it) }
}

fun downloadImageHttp(url: HttpUrl): Image? {
    return downloadMedia(url)?.let { Image(it.inputStream()) }
}

fun downloadMedia(url: HttpUrl): ByteArray? {
    val req = Request.Builder().url(url).build()
    val res = try {
        AppHttpClient.client.newCall(req).execute()
    } catch (err: Exception) {
        System.err.println("timeout getting $url, with error $err")
        return null
    }
    if (res.isSuccessful) {
        val reb: ResponseBody? = res.body()
        return reb?.bytes()
    }
    else {
        System.err.println("error code ${res.code()}, ${res.body()}")
        return null
    }
}
