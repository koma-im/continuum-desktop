package koma.network.matrix.media

import koma.storage.config.server.ServerConf
import koma.storage.config.server.getAddress
import koma.storage.config.server.getMediaPath
import okhttp3.HttpUrl

/**
 * convert mxc:// to https://
 */
fun ServerConf.mxcToHttp(mxc: String): HttpUrl? {
    val parts = mxc.substringAfter("mxc://")
    val serverName = parts.substringBefore('/')
    val media = parts.substringAfter('/')

    val hsAddr = this.getAddress()
    val serverUrl = HttpUrl.parse(hsAddr)!!
    val url =try {
        serverUrl.newBuilder()
                .addPathSegments(this.getMediaPath())
                .addPathSegment(serverName)
                .addPathSegment(media)
                .build()
    } catch (e: NullPointerException) {
        System.err.println("failed to convert $mxc using $hsAddr")
        e.printStackTrace()
        return null
    }
    return url
}
