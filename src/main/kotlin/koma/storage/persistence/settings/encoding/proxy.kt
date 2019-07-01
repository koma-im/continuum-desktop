package koma.storage.persistence.settings.encoding

import koma.util.KResult
import link.continuum.desktop.util.ErrorMsg
import link.continuum.desktop.util.Ok
import link.continuum.desktop.util.fmtErr
import java.io.Serializable
import java.net.InetSocketAddress
import java.net.Proxy

class ProxyList(proxies: List<KProxy> = listOf(KProxy.Direct)
): Serializable {
    private var proxies = proxies.toSet().toMutableList()

    companion object {
        const val serialVersionUID = 1L
    }

    fun default(): KProxy = proxies.getOrNull(0) ?: KProxy.Direct

    fun list(): List<KProxy> {
        val ps = proxies.toMutableList()
        if (!ps.contains(KProxy.Direct)) {
            ps.add(KProxy.Direct)
        }
        return ps
    }

    fun setDefault(p: KProxy): ProxyList {
        val ps = proxies.toMutableList()
        ps.remove(p)
        ps.add(0, p)
        return ProxyList(ps)
    }
}

sealed class KProxy: Serializable {
    object Direct: KProxy() {
        const val serialVersionUID = 1L
        override fun equals(other: Any?): Boolean {
            return other is Direct
        }
    }
    data class Http(val addr: InetSocketAddress): KProxy()
    data class Socks(val addr: InetSocketAddress): KProxy()

    companion object {
        const val serialVersionUID = 1L
        fun parse(type: Proxy.Type, host: String, port: String): KResult<KProxy, ErrorMsg> {
            if (type == Proxy.Type.DIRECT)  return Ok(KProxy.Direct)
            val portInt = port.toIntOrNull() ?: return fmtErr { "invalid port ${port}" }
            val sa = try {
                InetSocketAddress.createUnresolved(host, portInt)
            } catch (e: Exception) {
                return fmtErr { "invalid proxy address $host:$port, $e" }
            }
            return when(type) {
                Proxy.Type.HTTP -> Ok(KProxy.Http(sa))
                Proxy.Type.SOCKS -> Ok(KProxy.Socks(sa))
                Proxy.Type.DIRECT -> Ok(KProxy.Direct)
            }
        }
        fun parse(str: String): KResult<KProxy, Exception> {
            val proxyconf = str.split(" ", limit = 3)
            if (proxyconf.isEmpty()) return fmtErr { "proxy string $str is too short" }
            val ts = when(proxyconf[0].toUpperCase()) {
                "HTTP" -> Proxy.Type.HTTP
                "SOCKS" -> Proxy.Type.SOCKS
                "DIRECT" -> Proxy.Type.DIRECT
                else -> return fmtErr { "Invalid proxy type ${proxyconf[0]}" }
            }
            if (ts == Proxy.NO_PROXY) return KResult.of(KProxy.Direct)
            if (proxyconf.size < 3 ) return fmtErr { "proxy $str needs parameters" }
            return parse(ts, proxyconf[1], proxyconf[2])
        }
    }

    fun toJavaNet(): Proxy {
        return when (this) {
            is KProxy.Direct -> Proxy.NO_PROXY
            is KProxy.Http -> Proxy(Proxy.Type.HTTP, this.addr)
            is KProxy.Socks -> Proxy(Proxy.Type.SOCKS, this.addr)
        }
    }
}
