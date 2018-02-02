package koma.storage.config.server

import com.squareup.moshi.Moshi
import koma.storage.config.config_paths
import koma.storage.config.server.cert_trust.CompositeX509TrustManager
import koma.storage.config.server.cert_trust.loadContext
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import javax.net.ssl.SSLContext

class ServerConf(
        val servername: String,
        var addresses: MutableList<String>,
        val apiPath: String = "_matrix/client/r0/"
)

fun server_save_path(servername: String): String? {
    return config_paths.getOrCreate("settings", "homeserver", servername)
}

val conf_file_name = "server_conf.json"

/**
 * get preferred web address
 */
fun ServerConf.getAddress(): String {
    val addr = this.addresses.get(0)
    val slash = if (addr.endsWith('/')) addr else { addr.trimEnd('/') + "/" }
    return slash
}

fun ServerConf.saveAddress(addr: String) {
    this.addresses.remove(addr)
    this.addresses.add(0, addr)
    this.save()
}

fun ServerConf.loadCert(): Pair<SSLContext, CompositeX509TrustManager>? {
     val dir = server_save_path(
            this.servername)
    return dir?.let { loadContext(it) }
}

fun ServerConf.save() {
    val dir = server_save_path(
            this.servername)
    dir?: return
    val moshi = Moshi.Builder()
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

fun serverConfWithAddr(servername: String, addr: String): ServerConf{
    val conf = ServerConfiguration.getServerConf(servername)
    conf.saveAddress(addr)
    return conf
}
