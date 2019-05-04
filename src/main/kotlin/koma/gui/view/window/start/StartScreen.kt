package koma.gui.view.window.start

import javafx.geometry.Pos
import javafx.scene.layout.VBox
import tornadofx.*
import koma.gui.view.LoginScreen

class StartScreen(): View() {

    override val root = VBox()
    init {
        with(root) {
            alignment = Pos.CENTER
            hbox {
                alignment = Pos.CENTER
                add(LoginScreen())
            }
        }
    }
}
