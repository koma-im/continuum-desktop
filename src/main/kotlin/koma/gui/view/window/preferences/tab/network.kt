package koma.gui.view.window.preferences.tab

import javafx.collections.FXCollections
import javafx.scene.control.ComboBox
import javafx.scene.layout.VBox
import koma.gui.view.window.preferences.tab.network.AddProxyField
import koma.gui.view.window.preferences.tab.network.ExistingProxy
import koma.gui.view.window.preferences.tab.network.NewProxy
import koma.gui.view.window.preferences.tab.network.ProxyOption
import koma.koma_app.appState
import koma.storage.persistence.settings.AppSettings
import koma.util.getOr
import tornadofx.*


class NetworkSettingsTab(
        parent: View,
        private val settings: AppSettings = appState.store.settings
): View() {
    override val root = VBox()

    private val select: ComboBox<ProxyOption>

    private val proxyField = AddProxyField()

    init {
        val proxyOptions: List<ProxyOption> =  settings.proxyList.list().map { ExistingProxy(it) } + NewProxy()
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
            add(proxyField.root)

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
            proxyField.getProxy() getOr {return}
        }
        settings.proxyList = settings.proxyList.setDefault(proxy)
    }
}

