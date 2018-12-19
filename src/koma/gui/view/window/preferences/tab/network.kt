package koma.gui.view.window.preferences.tab

import javafx.collections.FXCollections
import javafx.scene.control.ComboBox
import javafx.scene.layout.VBox
import koma.gui.view.window.preferences.tab.network.AddProxyField
import koma.gui.view.window.preferences.tab.network.ExistingProxy
import koma.gui.view.window.preferences.tab.network.NewProxy
import koma.gui.view.window.preferences.tab.network.ProxyOption
import koma_app.appState
import tornadofx.*


class NetworkSettingsTab(parent: View): View() {
    override val root = VBox()

    private val select: ComboBox<ProxyOption>

    private val proxyField: AddProxyField by inject()

    init {
        val proxyOptions: List<ProxyOption> =  appState.settings.settings.proxies.map { ExistingProxy(it) } + NewProxy()
        select = ComboBox(FXCollections.observableArrayList(
                proxyOptions
        ))
        select.selectionModel.selectFirst()
        val creating = booleanBinding(select.valueProperty()) { value is NewProxy }
        proxyField.root.visibleWhen { creating }
        val selectedExisting = booleanBinding(select.valueProperty()) { value is ExistingProxy }
        val valid = selectedExisting.or(creating.and(proxyField.isValid))
        with(root) {
            spacing = 5.0
            label("Proxy Option")
            add(select)
            add(proxyField)

            buttonbar {
                button("Ok") {
                    enableWhen(valid)
                    action {
                        save()
                        parent.close()
                    }
                }
            }
        }
    }

    fun save() {
        val selection = select.value
        val proxy = if ( selection is ExistingProxy) {
            selection.proxy
        } else {
            proxyField.getProxy()
        }
        appState.settings.set_preferred_proxy(proxy)
        appState.settings.save()
    }
}

