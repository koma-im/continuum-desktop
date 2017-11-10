package koma.gui.view.window.preferences

import javafx.geometry.Side
import javafx.scene.control.TabPane
import javafx.scene.layout.VBox
import koma.gui.view.window.preferences.tab.NetworkSettingsTab
import koma_app.appState
import tornadofx.*

class PreferenceWindow(): View() {
    override val root = VBox()

    private val nettab = NetworkSettingsTab()

    init {
        val tabs = TabPane()
        with(tabs) {
            this.side  = Side.LEFT
            tab("Network") {
                add(nettab)
            }
        }
        with(root) {
            add(tabs)
            buttonbar {
                button("Ok") {
                    action {
                        save()
                        this@PreferenceWindow.close()
                    }
                }
            }
        }
        appState.apiClient?.userId?.server?.let { nettab.serverNameProperty.set(it) }
    }

    private fun save() {
        nettab.save()
    }
}
