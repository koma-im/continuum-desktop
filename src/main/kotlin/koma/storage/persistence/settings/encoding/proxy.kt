package koma.storage.persistence.settings.encoding

import koma.Failure
import koma.util.KResult
import link.continuum.desktop.util.Ok
import link.continuum.desktop.util.fmtErr
import java.net.InetSocketAddress
import java.net.Proxy

class ProxyList(
        private val map: MutableMap<String, Long>
) {
    fun default(): Proxy {
        return list().getOrNull(0) ?: Proxy.NO_PROXY
    }

    fun list(): List<Proxy> {
        val ps = map.asIterable().sortedByDescending {
            it.value
        }.mapNotNull { it.key.toProxyResult().getOrNull() }.toMutableList()
        if (!ps.contains(Proxy.NO_PROXY)) {
            ps.add(Proxy.NO_PROXY)
        }
        return ps
    }

    fun setDefault(p: Proxy) {
        val s = p.toCSV()
        map[s] = System.currentTimeMillis()
    }
}

/**
 * serialize proxy to string
 */
fun Proxy.toCSV(): String = buildString {
    val proxy = this@toCSV
    val t = when (proxy.type()) {
        Proxy.Type.DIRECT -> "DIRECT"
        Proxy.Type.HTTP -> "HTTP"
        Proxy.Type.SOCKS -> "SOCKS"
        null -> return@buildString
    }
    append(t)
    val a = proxy.address() ?: return@buildString
    if (a !is InetSocketAddress) return@buildString
    val h = a.hostString ?: return@buildString
    append(",")
    append(h)
    append(",")
    append(a.port)
}

fun parseProxyTriple(type: Proxy.Type, host: String, port: String): KResult<Proxy, Failure> {
    if (type == Proxy.Type.DIRECT)  return Ok(Proxy.NO_PROXY)
    val portInt = port.toIntOrNull() ?: return fmtErr { "invalid port ${port}" }
    val sa = try {
        InetSocketAddress.createUnresolved(host, portInt)
    } catch (e: Exception) {
        return fmtErr { "invalid proxy address $host:$port, $e" }
    }
    return KResult.success(Proxy(type, sa))
}

fun String.toProxyResult(): KResult<Proxy, Failure> {
    val str = this
    val proxyconf = str.split(",", limit = 3)
    if (proxyconf.isEmpty()) return fmtErr { "proxy string $str is too short" }
    val ts = when (proxyconf[0].toUpperCase()) {
        "HTTP" -> Proxy.Type.HTTP
        "SOCKS" -> Proxy.Type.SOCKS
        "DIRECT" -> Proxy.Type.DIRECT
        else -> return fmtErr { "Invalid proxy type ${proxyconf[0]}" }
    }
    if (ts == Proxy.Type.DIRECT) return KResult.success(Proxy.NO_PROXY)
    if (proxyconf.size < 3) return fmtErr { "proxy $str needs parameters" }
    return parseProxyTriple(ts, proxyconf[1], proxyconf[2])
}