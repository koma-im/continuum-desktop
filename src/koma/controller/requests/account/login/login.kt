package koma.controller.requests.account.login

import controller.LoginController
import javafx.scene.control.Alert
import koma.matrix.user.identity.UserId_new
import koma.storage.config.profile.Profile
import koma.storage.config.server.serverConfWithAddr
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.javafx.JavaFx
import kotlinx.coroutines.experimental.launch
import matrix.UserPassword
import matrix.login
import ru.gildor.coroutines.retrofit.Result
import ru.gildor.coroutines.retrofit.awaitResult
import tornadofx.*

/**
 * accept text of text fields as parameters
 */
fun doLogin(user: String, password: String, server: String, controller: LoginController) = async {
    val userid = UserId_new(user)
    val servCon = serverConfWithAddr(userid.server, server)
    val authedProfile: Profile = if (!password.isBlank()) {
        val authResu = login(UserPassword(user = userid.user, password = password), servCon).awaitResult()
        val auth: Profile =  when (authResu) {
            is Result.Ok -> Profile(authResu.value.user_id, authResu.value.access_token)
            is Result.Error -> {
                val ex = authResu.exception
                val mes = "${ex.code()} ${ex.message()}\n${ex.response()?.errorBody()?.string()}"
                System.err.println(mes)
                ex.printStackTrace()
                launch(JavaFx) {
                    alert(Alert.AlertType.ERROR, "Login Fail with Error",
                           mes)
                }
                return@async
            }
            is Result.Exception -> {
                authResu.exception.printStackTrace()
                launch(JavaFx) {
                    alert(Alert.AlertType.ERROR, "Login Fail with exception",
                            authResu.exception.message)
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
