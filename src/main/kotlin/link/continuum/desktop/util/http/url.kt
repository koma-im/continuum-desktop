package link.continuum.desktop.util.http

import koma.network.matrix.media.mxcToHttp
import mu.KotlinLogging
import okhttp3.HttpUrl

private val logger = KotlinLogging.logger {}

fun mapMxc(input: String, serverUrl: HttpUrl): HttpUrl? {
    if (input.startsWith("mxc://")) {
        return mxcToHttp(input, serverUrl)
    }
    val u = HttpUrl.parse(input)
    if (u == null) {
        logger.error { "invalid url $input belonging to server $serverUrl" }
    }
    return u
}
