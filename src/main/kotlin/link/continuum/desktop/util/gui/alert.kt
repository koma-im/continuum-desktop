package link.continuum.desktop.util.gui

import javafx.scene.control.Alert
import javafx.scene.control.ButtonType
import javafx.stage.Window
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.javafx.JavaFx

import kotlinx.coroutines.launch

fun alert(type: Alert.AlertType,
          header: String,
          content: String? = null,
          vararg buttons: ButtonType,
          owner: Window? = null,
          title: String? = null,
          actionFn: Alert.(ButtonType) -> Unit = {}) {
    GlobalScope.launch(Dispatchers.JavaFx) {
        val alert = Alert(type, content ?: "", *buttons)
        title?.let { alert.title = it }
        alert.headerText = header
        owner?.also { alert.initOwner(it) }
        val buttonClicked = alert.showAndWait()
        if (buttonClicked.isPresent) {
            alert.actionFn(buttonClicked.get())
        }
    }
}
