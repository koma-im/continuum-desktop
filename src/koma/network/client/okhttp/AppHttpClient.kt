package koma.network.client.okhttp

import koma.storage.config.server.ServerConf
import koma.storage.config.server.loadCert
import koma.storage.config.settings.AppSettings
import okhttp3.ConnectionPool
import okhttp3.OkHttpClient

/**
 * try to always reuse this client instead of creating a new one
 */
object AppHttpClient {
    val client: OkHttpClient
    val builder: OkHttpClient.Builder

    fun builderForServer(serverConf: ServerConf): OkHttpClient.Builder {
        val addtrust = serverConf.loadCert()
        return if (addtrust != null) {
            builder.sslSocketFactory(addtrust.first.socketFactory, addtrust.second)
        } else {
            builder
        }
    }

    init {
        val proxy = AppSettings.getProxy()
        val conpoo = ConnectionPool()
        builder = OkHttpClient.Builder()
                .proxy(proxy)
                .connectionPool(conpoo)
        client = setUpClient()
    }

    private fun setUpClient(): OkHttpClient {
        return builder.tryAddAppCache("http", 80*1024*1024)
                .trySetAppCert()
                .build()
    }
}
