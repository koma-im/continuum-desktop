package koma.gui.view.window.preferences

import javafx.geometry.Side
import javafx.scene.control.TabPane
import javafx.scene.layout.VBox
import koma.gui.view.window.preferences.tab.AppearanceTab
import koma.gui.view.window.preferences.tab.NetworkSettingsTab
import koma_app.AppSettings
import tornadofx.*

class PreferenceWindow(): View() {
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
    }

}
