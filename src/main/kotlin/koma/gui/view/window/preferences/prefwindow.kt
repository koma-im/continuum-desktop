package koma.gui.view.window.preferences

import javafx.geometry.Side
import javafx.scene.Scene
import javafx.scene.control.Tab
import javafx.scene.control.TabPane
import javafx.stage.Stage
import javafx.stage.Window
import koma.gui.view.window.preferences.tab.NetworkSettingsTab
import koma.koma_app.appState
import koma.storage.persistence.settings.AppSettings
import link.continuum.desktop.gui.VBox
import link.continuum.desktop.gui.add

class PreferenceWindow {
    val root = VBox()

    private val nettab = NetworkSettingsTab(this)
    private val stage = Stage()
    fun close() {
        stage.close()
    }

    fun openModal(owner: Window) {
        stage.initOwner(owner)
        stage.scene = Scene(root)
        stage.show()
    }
    init {
        val tabs = TabPane()
        with(tabs) {
            this.tabClosingPolicy = TabPane.TabClosingPolicy.UNAVAILABLE
            this.side  = Side.LEFT
            getTabs().addAll(
                    Tab("Network", nettab.root)
            )
        }
        with(root) {
            add(tabs)
        }
    }

}
