package koma.gui.view.window.preferences

import javafx.beans.binding.StringBinding
import javafx.geometry.Side
import javafx.scene.control.TabPane
import javafx.scene.layout.VBox
import koma.gui.view.window.preferences.tab.AppearanceTab
import koma.gui.view.window.preferences.tab.NetworkSettingsTab
import koma.storage.config.settings.AppSettings
import koma_app.appState
import tornadofx.*

class PreferenceWindow(server: StringBinding?=null): View() {
    override val root = VBox()

    private val nettab = NetworkSettingsTab(this)

    init {
        val tabs = TabPane()
        with(tabs) {
            this.tabClosingPolicy = TabPane.TabClosingPolicy.UNAVAILABLE
            this.side  = Side.LEFT
            tab("Network") {
                add(nettab)
            }
            tab("Appearance") {
                add(AppearanceTab(this@PreferenceWindow))
            }
        }
        with(root) {
            style {
                fontSize= AppSettings.settings.scaling.em
            }
            add(tabs)
        }
        if (server != null) {
            nettab.serverNameProperty.bind(server)
        } else {
            appState.apiClient?.userId?.server?.let { nettab.serverNameProperty.set(it) }
        }
    }

}
