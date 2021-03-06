package koma.gui.view.window.preferences.tab.network

import javafx.beans.binding.BooleanBinding
import javafx.collections.FXCollections
import javafx.scene.control.ComboBox
import javafx.scene.control.TextField
import koma.Failure
import koma.storage.persistence.settings.encoding.parseProxyTriple
import koma.util.KResult
import link.continuum.desktop.gui.HBox
import link.continuum.desktop.gui.add
import link.continuum.desktop.gui.booleanBinding
import java.net.Proxy


class AddProxyField {
    val root = HBox()

    private val type = ComboBox<Proxy.Type>()
    private val host = TextField()
    private val port = TextField()

    val isValid: BooleanBinding

    fun getProxy(): KResult<Proxy, Failure> {
        return parseProxyTriple(type.value, host.text, port.text)
    }

    init {
        val typeValid = booleanBinding(type.valueProperty()) { value != null }
        val hostValid = booleanBinding(host.textProperty()) { value?.isNotBlank() ?: false }
        val portValid = booleanBinding(port.textProperty()) { value?.toIntOrNull() != null }
        isValid = typeValid.and(hostValid).and(portValid)

        with(root)   {
            spacing = 5.0

            type.items = FXCollections.observableArrayList(
                    Proxy.Type.values().filterNot { it == Proxy.Type.DIRECT }.toList())
            type.selectionModel.selectFirst()
            add(type)

            host.promptText = "address"
            add(host)

            port.promptText = "port"
            add(port)
        }
    }
}
