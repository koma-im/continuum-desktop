package koma.gui.view.window.preferences.tab.network

import koma.storage.persistence.settings.encoding.KProxy
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

class ExistingProxy(val proxy: KProxy): ProxyOption()

class NewProxy(): ProxyOption()




private fun KProxy.toAddrStr(): String {
    logger.debug { "proxy $this" }
    return when(this) {
        is KProxy.Direct -> "No Proxy"
        is KProxy.Socks -> "socks://${this.addr}"
        is KProxy.Http -> "http://${this.addr}"
    }
}
