package koma.controller.requests.account.login

import com.github.kittinunf.result.Result
import com.squareup.moshi.JsonEncodingException
import javafx.scene.control.Alert
import koma.Koma
import koma.gui.view.window.auth.login.startChat
import koma.koma_app.appState
import koma.matrix.UserPassword
import koma.matrix.login
import koma.matrix.user.identity.UserId_new
import koma.storage.config.profile.loadUser
import koma.storage.persistence.account.Token
import koma.storage.persistence.account.saveToken
import koma.util.coroutine.adapter.retrofit.awaitMatrix
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.javafx.JavaFx
import kotlinx.coroutines.launch
import tornadofx.*

/**
 * accept text of text fields as parameters
 */
suspend fun Koma.doLogin(user: String, password: String, server: String) {
    val userid = UserId_new(user)
    appState.currentUser = userid
    val servCon = this.servers.serverConfWithAddr(userid.server, server)
    if (!password.isBlank()) {
        val authResu = login(UserPassword(user = userid.user, password = password), servCon, this.http).awaitMatrix()
        when (authResu) {
            is Result.Success -> {
                val u = authResu.value.user_id
                val t = authResu.value.access_token
                saveToken(this.paths, u, Token(t))
                loadUser(this, userid)
                startChat(this, userid, t, servCon)
            }
            is Result.Failure -> {
                val ex = authResu.error
                val mes = ex.message
                System.err.println(mes)
                val message = if (ex is JsonEncodingException) {
                    "Does $server have a valid JSON API?"
                } else mes
                GlobalScope.launch(Dispatchers.JavaFx) {
                    alert(Alert.AlertType.ERROR, "Login Fail with Error",
                           message)
                }
                return
            }
        }
    } else {
        val p = loadUser(this, userid)
        if (p == null) {
            GlobalScope.launch(Dispatchers.JavaFx) {
                alert(Alert.AlertType.ERROR, "Failed to login as $userid",
                        "No access token")
            }
            return
        }
        startChat(this, userid, p.token, servCon)
    }
}
