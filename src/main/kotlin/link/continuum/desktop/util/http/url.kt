package link.continuum.desktop.util.http

import koma.network.matrix.media.mxcToHttp
import okhttp3.HttpUrl

fun mapMxc(input: String, serverUrl: HttpUrl): HttpUrl? {
    if (input.startsWith("mxc://")) {
        return mxcToHttp(input, serverUrl)
    }
    return HttpUrl.parse(input)
}
