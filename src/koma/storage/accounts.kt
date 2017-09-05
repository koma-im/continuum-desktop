package koma.storage

import com.moandjiezana.toml.Toml
import com.moandjiezana.toml.TomlWriter
import util.getCreateAppDataDir
import java.io.File
import java.io.IOException

object Recent {
    val server_addrs: Map<String, List<String>>

    init {
        server_addrs = load_recent_servers()
    }

    private fun load_recent_servers(): Map<String, List<String>> {
        var addrs = mutableMapOf<String, List<String>>()
        val auth_dir = getCreateAppDataDir("auth")?.let { File(it) }
        if (auth_dir == null) {
            return addrs
        }
        for (f in auth_dir.listFiles()) {
            if (!f.isDirectory) continue
            val af = f.resolve(".address.toml")
            if (!af.isFile) continue
            val toml = Toml().read(af)
            toml.getList<String?>("addresses")?.let { addrs.put(f.name, it.filterNotNull()) }
        }
        return addrs
    }

}

fun save_server_address(name: String, address: String) {
    println("saving address $address of server $name")
    val server_dir = getCreateAppDataDir("auth", name)
    if (server_dir != null) {
        val addrs = mutableListOf<String>(address)

        Recent.server_addrs.get(name)?.let { addrs.addAll(it) }
        val addr_file = server_dir + File.separator + ".address.toml"
        val file = File(addr_file)

        val data = mapOf(Pair("addresses", addrs.map{ it.trimEnd('/') }.distinct().take(10).map { it.toString() }))
        val tomlWriter = TomlWriter()
        try {
            tomlWriter.write(data, file)
        } catch (e: IOException) {
            println("failed to save server $name address $address")
        }
    } else {
        println("failed to save latest use")
    }
}

