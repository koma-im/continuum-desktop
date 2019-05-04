package koma.gui.view.window.userinfo.actions

import com.github.kittinunf.result.Result
import javafx.stage.FileChooser
import koma.controller.requests.media.uploadFile
import koma.koma_app.appState
import koma.matrix.user.AvatarUrl
import koma.util.coroutine.adapter.retrofit.awaitMatrix
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.javafx.JavaFx
import kotlinx.coroutines.launch
import org.controlsfx.control.Notifications
import tornadofx.*

fun chooseUpdateUserAvatar() {
    val api = appState.apiClient
    api ?: return
    val dialog = FileChooser()
    dialog.title = "Upload a new avatar"
    val file = dialog.showOpenDialog(FX.primaryStage)
    file ?: return
    GlobalScope.launch {
        val upload = uploadFile(api, file)
        when (upload) {
            is Result.Failure -> {
                val message = upload.error.message
                launch(Dispatchers.JavaFx) {
                    Notifications.create()
                            .title("Failed to upload new avatar")
                            .text(message.toString())
                            .owner(FX.primaryStage)
                            .showWarning()
                }
            }
            is Result.Success -> {
                val data = AvatarUrl(upload.value.content_uri)
                val result = api.updateAvatar(api.userId, data).awaitMatrix()
                if (result is Result.Failure) {
                    val message = result.error.message
                    launch(Dispatchers.JavaFx) {
                        Notifications.create()
                                .title("Failed to set new avatar")
                                .text(message.toString())
                                .owner(FX.primaryStage)
                                .showWarning()
                    }
                }
            }
        }
    }
}
