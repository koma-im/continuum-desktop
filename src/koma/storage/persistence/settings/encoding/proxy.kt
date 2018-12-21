package koma.storage.persistence.settings.encoding

import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson
import java.net.InetSocketAddress
import java.net.Proxy


class ProxyAdapter {
    @ToJson
    fun toJson(proxy: Proxy): String {
        val sb = StringBuilder(proxy.type().toString())
        val addr = proxy.address() as InetSocketAddress?
        if (addr != null) {
            val host = addr.hostString
            if (host != null) sb.append(" " + host)
            val port = addr.port
            sb.append(" " + port)
        }
        return sb.toString()
    }

    @FromJson
    fun fromJson(str: String): Proxy {
        val proxyconf = str.split(" ", limit = 3)
        if (proxyconf.size < 1 ) return Proxy.NO_PROXY
        if (proxyconf[0] == "DIRECT" ) return Proxy.NO_PROXY
        if (proxyconf.size < 3 ) return Proxy.NO_PROXY
        val type = when(proxyconf[0]) {
            "HTTP" -> Proxy.Type.HTTP
            "SOCKS" -> Proxy.Type.SOCKS
            else -> return Proxy.NO_PROXY
        }
        val host = proxyconf[1]
        val port = proxyconf[2].toIntOrNull()
        if (port == null) return Proxy.NO_PROXY
        val sa = InetSocketAddress.createUnresolved(host, port)
        return Proxy(type, sa)
    }
}


