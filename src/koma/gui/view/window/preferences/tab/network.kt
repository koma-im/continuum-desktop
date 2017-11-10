package koma.gui.view.window.preferences.tab

import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.scene.Parent
import javafx.scene.control.ComboBox
import javafx.scene.control.TextField
import koma.storage.config.server.get_server_proxy
import koma.storage.config.server.save_server_proxy
import tornadofx.*
import java.net.InetSocketAddress
import java.net.Proxy

class NetworkSettingsTab(): View() {
    override val root: Parent = Fieldset()

    val serverNameProperty = SimpleStringProperty()

    private val type = ComboBox<Proxy.Type>()
    private val host = TextField()
    private val port = TextField()

    init {
        with(root) {
            form {
                fieldset("Proxy") {
                    field("type") {
                        type.items = FXCollections.observableArrayList(Proxy.Type.values().toList())
                        add(type)
                    }
                    field("host") {
                        add(host)
                    }
                    field("port") {
                        add(port)
                    }
                }
            }
        }
        serverNameProperty.onChange { if (it != null && it.isNotBlank()) load(it) }
    }

    fun save() {
        val conf = getConf()
        if (conf != null)
         save_server_proxy(serverNameProperty.get(), conf)
        else
            println("null conf")
    }

    fun load(serverName: String) {
        val conf = get_server_proxy(serverName)
        if (conf == null) return
        val proxytype = conf.type()
        type.value = proxytype
        val addr = conf.address() as InetSocketAddress?
        addr?.let {
            host.text = it.hostString
            port.text = it.port.toString()
        }
    }

    fun getConf(): Proxy? {
        if (type.value == Proxy.Type.DIRECT)
            return Proxy.NO_PROXY
        val port = this.port.text.toIntOrNull()
        if (port == null) return null
        val sock = InetSocketAddress(host.text, port)
        return Proxy(type.value, sock)
    }
}

