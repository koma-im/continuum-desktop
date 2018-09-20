package koma.gui.view.window.userinfo.actions

import com.github.kittinunf.result.Result
import javafx.scene.control.TextInputDialog
import koma.util.coroutine.adapter.retrofit.awaitMatrix
import koma_app.appState.apiClient
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
        when (result) {
            is Result.Failure -> {
                launch(Dispatchers.JavaFx) {
                    Notifications.create()
                            .title("Failed to update nick name")
                            .text(result.error.message.toString())
                            .owner(FX.primaryStage)
                            .showWarning()
                }
            }
        }
    }
}
