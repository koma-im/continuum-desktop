package koma.controller.requests.account.login

import com.github.kittinunf.result.Result
import com.squareup.moshi.JsonEncodingException
import javafx.scene.control.Alert
import koma.Koma
import koma.matrix.UserId
import koma.matrix.UserPassword
import koma.matrix.login
import koma.matrix.user.identity.UserId_new
import koma.storage.config.ConfigPaths
import koma.storage.config.server.ServerConf
import koma.storage.config.server.getAddress
import koma.storage.persistence.account.Token
import koma.storage.persistence.account.getToken
import koma.storage.persistence.account.saveToken
import koma.util.coroutine.adapter.retrofit.awaitMatrix
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.javafx.JavaFx
import kotlinx.coroutines.launch
import link.continuum.desktop.action.startChat
import tornadofx.*

/**
 * when the login button is clicked
 * accept text of text fields as parameters
 */
suspend fun onClickLogin(koma: Koma, user: String, password: String, server: String) {
    val userid = UserId_new(user)
    val servCon = koma.servers.serverConfWithAddr(userid.server, server)
    val token = if (!password.isBlank()) {
        getTokenWithPassword(userid, password, koma, servCon)
    } else {
        getTokenFromDisk(koma.paths, userid)
    }
    token ?: return
    startChat(koma, userid, token, servCon)
}

/**
 * try to load saved token
 * show alert if it fails
 */
private fun getTokenFromDisk(paths: ConfigPaths, userid: UserId): String? {
    val p = getToken(paths, userid)
    if (p == null) {
        GlobalScope.launch(Dispatchers.JavaFx) {
            alert(Alert.AlertType.ERROR, "Failed to login as $userid",
                    "No access token")
        }
        return null
    }
    return p.token
}

/**
 * get token from server
 * saves the token to disk
 */
private suspend fun getTokenWithPassword(userid: UserId, password: String, koma: Koma, servCon: ServerConf): String? {
    val authResu = login(UserPassword(user = userid.user, password = password), servCon, koma.http).awaitMatrix()
    when (authResu) {
        is Result.Success -> {
            val u = authResu.value.user_id
            val t = authResu.value.access_token
            saveToken(koma.paths, u, Token(t))
            return t
        }
        is Result.Failure -> {
            val ex = authResu.error
            val mes = ex.message
            System.err.println(mes)
            val message = if (ex is JsonEncodingException) {
                "Does ${servCon.getAddress()} have a valid JSON API?"
            } else mes
            GlobalScope.launch(Dispatchers.JavaFx) {
                alert(Alert.AlertType.ERROR, "Login Fail with Error",
                        message)
            }
        }
    }
    return null
}
