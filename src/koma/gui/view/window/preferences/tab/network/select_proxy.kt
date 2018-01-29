package koma.gui.view.window.preferences.tab.network

import java.net.InetSocketAddress
import java.net.Proxy

sealed class ProxyOption() {
    override fun toString(): String {
        return when(this) {
            is ExistingProxy -> this.proxy.toAddrStr()
            is NewProxy -> "Add New Proxy"
        }
    }

    fun isNoProxy(): Boolean {
        return this is ExistingProxy && this.proxy == Proxy.NO_PROXY
    }
}

class ExistingProxy(val proxy: Proxy): ProxyOption()

class NewProxy(): ProxyOption()




private fun Proxy.toAddrStr(): String {
    if (this == Proxy.NO_PROXY) {
        return "No Proxy"
    }

    val proto = this.type().toString().toLowerCase()
    val sb = StringBuilder(proto)
    val addr = this.address() as InetSocketAddress?
    if (addr != null) {
        val host = addr.hostString
        if (host != null) sb.append("://" + host)
        val port = addr.port
        sb.append(":" + port)
    }
    return sb.toString()
}
