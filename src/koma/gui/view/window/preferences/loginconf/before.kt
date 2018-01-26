package koma.gui.view.window.preferences.loginconf

import javafx.beans.binding.StringBinding
import javafx.scene.layout.VBox
import koma.gui.view.window.preferences.tab.NetworkSettingsTab
import koma.storage.config.settings.AppSettings
import tornadofx.*

/**
 * configure additional options before login
 */
class LoginConfWindow(serverName: StringBinding): View() {
    override val root = VBox()

    private val nettab = NetworkSettingsTab(this)

    init {
        nettab.serverNameProperty.bind(serverName)
        with(root) {
            style {
                fontSize= AppSettings.settings.fontSize.em
            }
            add(nettab)
            buttonbar {
                button("Ok") {
                    action {
                        nettab.save()
                    }
                }
                button("Cancel") {
                    action { this@LoginConfWindow.close() }
                }
            }
        }
    }
}
