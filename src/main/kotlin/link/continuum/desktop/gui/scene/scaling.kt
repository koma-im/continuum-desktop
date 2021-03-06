package link.continuum.desktop.gui.scene

import javafx.scene.Node
import javafx.scene.input.KeyEvent
import javafx.scene.text.Font
import link.continuum.desktop.gui.StackPane
import link.continuum.desktop.util.debugAssertUiThread

class ScalingPane(
        private var fontSize: Double? = null
) {
    val root = StackPane()
    fun getFontSize(): Double {
        return fontSize ?: Font.getDefault().size
    }
    fun setChild (node: Node) {
        debugAssertUiThread()
        root.children.setAll(node)
    }
    init {
        root.addEventFilter(KeyEvent.KEY_TYPED) {
            if (it.isControlDown && (it.character == "+" || it.character == "=")){
                val fs = getFontSize() + 1.0
                root.style = "-fx-font-size: ${fs}px;"
                fontSize = fs
                it.consume()
            } else if (it.isControlDown && (it.character == "-" )){
                val fs = getFontSize() - 1.0
                root.style = "-fx-font-size: ${fs}px;"
                fontSize = fs
                it.consume()
            }
        }
    }
}
