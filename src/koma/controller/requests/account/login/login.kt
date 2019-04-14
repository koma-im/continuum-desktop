package koma.controller.requests.account.login

import com.github.kittinunf.result.Result
import com.squareup.moshi.JsonEncodingException
import javafx.scene.control.Alert
import koma.Koma
import koma.matrix.UserId
import koma.matrix.UserPassword
import koma.matrix.login
import koma.matrix.user.identity.UserId_new
import koma.util.coroutine.adapter.retrofit.awaitMatrix
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.javafx.JavaFx
import kotlinx.coroutines.launch
import link.continuum.desktop.action.startChat
import link.continuum.database.KDataStore
import link.continuum.database.models.getToken
import link.continuum.database.models.saveServerAddr
import link.continuum.database.models.saveToken
import link.continuum.desktop.util.gui.alert
import okhttp3.HttpUrl


/**
 * when the login button is clicked
 * accept text of text fields as parameters
 */
suspend fun onClickLogin(koma: Koma, data: KDataStore, user: String, password: String, server: String) {
    val userid = UserId_new(user)
    val url = HttpUrl.parse(server)
    if (url == null) {
        alert(Alert.AlertType.ERROR, "Invalid server url",
                "$server not parsed")
        return
    }
    saveServerAddr(data, userid.server, server)
    val token = if (!password.isBlank()) {
        getTokenWithPassword(userid, password, koma, data, server)
    } else {
        val t = getToken(data, userid)
        if (t == null) {
            GlobalScope.launch(Dispatchers.JavaFx) {
                alert(Alert.AlertType.ERROR, "Failed to login as $userid",
                        "No access token")
            }
        }
        t
    }
    token ?: return
    startChat(koma, userid, token, url, data)
}

/**
 * get token from server
 * saves the token to disk
 */
private suspend fun getTokenWithPassword(userid: UserId, password: String, koma: Koma,
                                         data: KDataStore,
                                         server: String): String? {
    val authResu = login(UserPassword(user = userid.user, password = password), server, koma.http).awaitMatrix()
    when (authResu) {
        is Result.Success -> {
            val u = authResu.value.user_id
            val t = authResu.value.access_token
            saveToken(data, u, t)
            return t
        }
        is Result.Failure -> {
            val ex = authResu.error
            val mes = ex.message
            System.err.println(mes)
            val message = if (ex is JsonEncodingException) {
                "Does ${server} have a valid JSON API?"
            } else mes
            GlobalScope.launch(Dispatchers.JavaFx) {
                alert(Alert.AlertType.ERROR, "Login Fail with Error",
                        message)
            }
        }
    }
    return null
}
