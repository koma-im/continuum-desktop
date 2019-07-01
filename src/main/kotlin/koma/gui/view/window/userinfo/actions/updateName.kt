package koma.gui.view.window.userinfo.actions

import javafx.scene.control.TextInputDialog
import koma.util.coroutine.adapter.retrofit.awaitMatrix
import koma.koma_app.appState.apiClient
import koma.util.onFailure
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.javafx.JavaFx
import kotlinx.coroutines.launch
import org.controlsfx.control.Notifications
import tornadofx.*

fun updateMyAlias() {
    val dia = TextInputDialog()
    dia.title = "Update my alias"
    val res = dia.showAndWait()
    val newname = if (res.isPresent && res.get().isNotBlank())
        res.get()
    else
        return
    val api = apiClient
    api?:return
    GlobalScope.launch {
        val result = api.updateDisplayName(newname).awaitMatrix()
        result.onFailure {
            launch(Dispatchers.JavaFx) {
                Notifications.create()
                        .title("Failed to update nick name")
                        .text(it.message.toString())
                        .owner(FX.primaryStage)
                        .showWarning()
            }
        }
    }
}
