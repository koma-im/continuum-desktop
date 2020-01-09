package koma.gui.view.window.preferences.tab.network

import mu.KotlinLogging
import java.net.Proxy

private val logger = KotlinLogging.logger {}

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
    logger.debug { "proxy $this" }
    return when(this.type()) {
        Proxy.Type.DIRECT -> "No Proxy"
        Proxy.Type.SOCKS -> "socks://${this.address()}"
        Proxy.Type.HTTP -> "http://${this.address()}"
        null -> this.toString()
    }
}
