package koma.network.client.okhttp

import koma.storage.config.settings.AppSettings
import okhttp3.OkHttpClient

/**
 * try to always reuse this client instead of creating a new one
 */
object AppHttpClient {
    val client: OkHttpClient

    init {
        client = setUpClient()
    }

    private fun setUpClient(): OkHttpClient {
        val proxy = AppSettings.getProxy()

        val ob = OkHttpClient.Builder()
                .proxy(proxy)
                .tryAddAppCache("http", 80*1024*1024)
                .tryAddAppCert()
        return ob.build()
    }
}
