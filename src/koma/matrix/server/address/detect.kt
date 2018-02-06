package koma.matrix.server.address

import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * given a homeserver name, try to find an address that is working
 */
suspend fun tryFindServerAddress(client: OkHttpClient, name: String): HttpUrl? {
    val hb = HttpUrl.Builder()
            .scheme("https")
            .host(name)
            .addPathSegments("_matrix/client/versions")
    val url443 = hb.port(443).build();
    if (responseOk(client, url443)) return url443
    val url8448 = hb.port(8448).build();
    if (responseOk(client, url8448)) return url8448
    return null
}

suspend fun responseOk(client: OkHttpClient, url: HttpUrl): Boolean {
    val req = Request.Builder().url(url).build()
    client.newCall(req)
    // TODO actually make the call using coroutines
    return false
}
