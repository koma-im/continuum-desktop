package koma.storage.config.server

import com.squareup.moshi.Moshi
import koma.storage.config.config_paths
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.net.Proxy
import java.util.concurrent.ConcurrentHashMap

class ServerConf(
        val servername: String,
        var addresses: MutableList<String>,
        var proxies: MutableList<Proxy> = mutableListOf(Proxy.NO_PROXY),
        val apiPath: String = "_matrix/client/r0/"
)

fun server_save_path(servername: String): String? {
    return config_paths.getOrCreate("settings", "homeserver", servername)
}

val conf_file_name = "server_conf.json"

fun ServerConf.saveProxy(proxy: Proxy) {
    this.proxies.remove(proxy)
    this.proxies.add(0, proxy)
    this.save()
}

fun ServerConf.saveAddress(addr: String) {
    this.addresses.remove(addr)
    this.addresses.add(0, addr)
    this.save()
}

fun ServerConf.save() {
    val dir = server_save_path(
            this.servername)
    dir?: return
    val moshi = Moshi.Builder()
            .add(ProxyAdapter())
            .build()
    val jsonAdapter = moshi.adapter(ServerConf::class.java).indent("    ")
    val json = try {
        jsonAdapter.toJson(this)
    } catch (e: ClassCastException) {
        e.printStackTrace()
        return
    }
    try {
        val file = File(dir).resolve(conf_file_name)
        file.writeText(json)
    } catch (e: IOException) {
    }
}

private fun computeServerConf(servername: String): ServerConf {
   val serverConf = ServerConf(
            servername,
            mutableListOf()
            )
    val dir = server_save_path(
            servername)
    dir?: return serverConf
    val sf = File(dir).resolve(conf_file_name)
    val jsonAdapter = Moshi.Builder()
            .add(ProxyAdapter())
            .build()
            .adapter(ServerConf::class.java)
    val loadedConf = try {
        jsonAdapter.fromJson(sf.readText())
    } catch (e: FileNotFoundException) {
        null
    } catch (e: IOException) {
        e.printStackTrace()
        null
    }
    return  loadedConf?: serverConf
}

private object ServerConfiguration {
    private val configurations = ConcurrentHashMap<String, ServerConf>()

    fun getServerConf(servername: String): ServerConf {
        val conf = configurations.computeIfAbsent(servername, { computeServerConf(it) })
        return conf
    }
}

fun loadServerConf(servername: String): ServerConf{
    return ServerConfiguration.getServerConf(servername)
}
