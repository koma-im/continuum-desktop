package koma.controller.requests.account.login

import javafx.scene.control.Alert
import koma.InvalidData
import koma.Server
import koma.koma_app.AppData
import koma.matrix.UserId
import koma.matrix.UserPassword
import koma.util.testFailure
import kotlinx.coroutines.*
import kotlinx.coroutines.javafx.JavaFx
import link.continuum.desktop.action.startChat
import link.continuum.desktop.database.KeyValueStore
import link.continuum.desktop.util.gui.alert
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient

/**
 * when the login button is clicked
 * accept text of text fields as parameters
 */
suspend fun onClickLogin(httpClient: OkHttpClient,
                         appData: Deferred<AppData>,
                         userid: UserId, password: String, server: String,
                         keyValueStore: KeyValueStore
) {
    val url = server.toHttpUrlOrNull()
    if (url == null) {
        alert(Alert.AlertType.ERROR, "Invalid server url",
                "$server not parsed")
        return
    }
    keyValueStore.serverToAddress.put(userid.server, server)
    val client = Server(url, httpClient)
    val token = if (!password.isBlank()) {
        getTokenWithPassword(userid, password, keyValueStore, client)
    } else {
        val t =keyValueStore.userToToken.get(userid.full)
        if (t == null) {
            GlobalScope.launch(Dispatchers.JavaFx) {
                alert(Alert.AlertType.ERROR, "Failed to login as $userid",
                        "No access token")
            }
        }
        t
    }
    keyValueStore.activeAccount.put(userid.full)
    token ?: return
    withContext(Dispatchers.Main) {
        startChat(httpClient,
                userid, token, url,
                keyValueStore,
                appData)
    }
}

/**
 * get token from server
 * saves the token to disk
 */
private suspend fun getTokenWithPassword(userid: UserId, password: String,
                                         keyValueStore: KeyValueStore,
                                         server: Server): String? {
    val (it, ex, result) = server.login(UserPassword(user = userid.user, password = password))
    if (!result.testFailure(it, ex)) {
        val u = it.user_id
        val t = it.access_token
        keyValueStore.userToToken.put(u.full, t)
        return t
    } else {
        val mes = ex.toString()
        System.err.println(mes)
        val message = if (ex is InvalidData) {
            "Does ${server} have a valid JSON API?"
        } else mes
        GlobalScope.launch(Dispatchers.JavaFx) {
            alert(Alert.AlertType.ERROR, "Login Fail with Error",
                    message)
        }
    }
    return null
}
