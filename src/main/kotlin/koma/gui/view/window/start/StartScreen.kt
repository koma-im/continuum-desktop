package koma.gui.view.window.start

import javafx.geometry.Pos
import javafx.scene.layout.HBox
import javafx.scene.layout.StackPane
import javafx.scene.layout.VBox
import koma.gui.view.LoginScreen
import org.controlsfx.control.MaskerPane
import tornadofx.View

class StartScreen(): View() {

    override val root = StackPane()
    private val mask = MaskerPane().apply {
        isVisible = false
    }

    init {
        title = "Continuum"
        root.children.addAll(
                VBox().apply {
                    alignment = Pos.CENTER
                    children.add(HBox().apply {
                        alignment = Pos.CENTER
                        children.add(LoginScreen(mask).root)
                    })
                },
                mask)
    }
}
