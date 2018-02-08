package koma.network.client.okhttp

import koma.storage.config.config_paths
import koma.storage.config.server.cert_trust.loadContext
import okhttp3.OkHttpClient

fun OkHttpClient.Builder.tryAddAppCert(): OkHttpClient.Builder {
    val certdir = config_paths.getCreateDir("settings") ?: return this
    val ctx = loadContext(certdir) ?: return this
    return  this.sslSocketFactory(ctx.first.socketFactory, ctx.second)
}
