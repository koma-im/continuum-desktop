package koma.network.client.okhttp

import koma.storage.config.config_paths
import okhttp3.Cache
import okhttp3.OkHttpClient

fun OkHttpClient.Builder.tryAddAppCache(name: String, size:Long): OkHttpClient.Builder {
    val cachedir = config_paths.getCreateDir("data", "cache", name)
    cachedir ?: return this
    return this.cache(Cache(cachedir, size))
}
