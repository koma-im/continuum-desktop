package koma.controller.requests.account

import com.github.kittinunf.result.Result
import com.squareup.moshi.JsonEncodingException
import controller.LoginController
import javafx.scene.control.Alert
import koma.matrix.UserId
import koma.storage.config.profile.Profile
import koma.storage.config.server.serverConfWithAddr
import koma.util.coroutine.adapter.retrofit.awaitMatrix
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.javafx.JavaFx
import kotlinx.coroutines.launch
import matrix.UserRegistering
import matrix.register
import tornadofx.*

suspend fun registerUser(controller: LoginController, userId: UserId, password: String, server: String) {
    val data = UserRegistering(userId.user, password)
    val s = serverConfWithAddr(userId.server, server)
    val r = register(data, s).awaitMatrix()
    when (r) {
        is Result.Failure -> {
            GlobalScope.launch(Dispatchers.JavaFx) {
                val content = if (r.error is JsonEncodingException) {
                    "Is $server really a matrix server with JSON API?"
                } else {
                    null
                }
                alert(Alert.AlertType.ERROR, "Register Failure: ${r.error.message}", content)
            }
            return
        }
        is Result.Success -> {
            val u = r.value
            val prof = Profile(u.user_id, u.access_token)
            GlobalScope.launch(Dispatchers.JavaFx) {
                controller.postLogin(prof, s)
            }
        }
    }
}
