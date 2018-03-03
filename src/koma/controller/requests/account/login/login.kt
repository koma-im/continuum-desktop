package koma.controller.requests.account.login

import com.github.kittinunf.result.Result
import controller.LoginController
import javafx.scene.control.Alert
import koma.matrix.user.identity.UserId_new
import koma.storage.config.profile.Profile
import koma.storage.config.server.serverConfWithAddr
import koma.util.coroutine.adapter.retrofit.awaitMatrix
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.javafx.JavaFx
import kotlinx.coroutines.experimental.launch
import matrix.UserPassword
import matrix.login
import tornadofx.*

/**
 * accept text of text fields as parameters
 */
fun doLogin(user: String, password: String, server: String, controller: LoginController) = async {
    val userid = UserId_new(user)
    val servCon = serverConfWithAddr(userid.server, server)
    val authedProfile: Profile = if (!password.isBlank()) {
        val authResu = login(UserPassword(user = userid.user, password = password), servCon).awaitMatrix()
        val auth: Profile =  when (authResu) {
            is Result.Success -> Profile(authResu.value.user_id, authResu.value.access_token)
            is Result.Failure -> {
                val ex = authResu.error
                val mes = ex.message
                System.err.println(mes)
                ex.printStackTrace()
                launch(JavaFx) {
                    alert(Alert.AlertType.ERROR, "Login Fail with Error",
                           mes)
                }
                return@async
            }
        }
        auth
    } else {
        val p = Profile.new(userid)
        if (p == null) {
            launch(JavaFx) {
                alert(Alert.AlertType.ERROR, "Failed to login as $userid",
                        "No access token")
            }
            return@async
        }
        p
    }
    launch(JavaFx) {
        controller.postLogin(authedProfile, servCon)
    }
}
