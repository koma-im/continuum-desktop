package koma.controller.requests.account

import com.github.kittinunf.result.Result
import controller.LoginController
import javafx.scene.control.Alert
import koma.matrix.UserId
import koma.storage.config.profile.Profile
import koma.storage.config.server.serverConfWithAddr
import koma.util.coroutine.adapter.retrofit.awaitMatrix
import kotlinx.coroutines.experimental.javafx.JavaFx
import kotlinx.coroutines.experimental.launch
import matrix.UserRegistering
import matrix.register
import tornadofx.*

suspend fun registerUser(controller: LoginController, userId: UserId, password: String, server: String) {
    val data = UserRegistering(userId.user, password)
    val s = serverConfWithAddr(userId.server, server)
    val r = register(data, s).awaitMatrix()
    when (r) {
        is Result.Failure -> {
            launch(JavaFx) {
                alert(Alert.AlertType.ERROR, "Register Failure: ${r.error.message}")
            }
            return
        }
        is Result.Success -> {
            val u = r.value
            val prof = Profile(u.user_id, u.access_token)
            launch(JavaFx) {
                controller.postLogin(prof, s)
            }
        }
    }
}
