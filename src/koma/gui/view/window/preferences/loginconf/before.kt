package koma.gui.view.window.preferences.loginconf

import javafx.beans.binding.StringBinding
import javafx.scene.layout.VBox
import koma.gui.view.window.preferences.tab.NetworkSettingsTab
import tornadofx.*

/**
 * configure additional options before login
 */
class LoginConfWindow(serverName: StringBinding): View() {
    override val root = VBox()

    private val nettab = NetworkSettingsTab()

    init {
        nettab.serverNameProperty.bind(serverName)
        with(root) {
            add(nettab)
            buttonbar {
                button("Ok") {
                    action {
                        nettab.save()
                        this@LoginConfWindow.close()
                    }
                }
                button("Cancel") {
                    action { this@LoginConfWindow.close() }
                }
            }
        }
    }
}
