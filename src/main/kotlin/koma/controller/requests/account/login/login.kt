package koma.controller.requests.account.login

import com.squareup.moshi.JsonEncodingException
import javafx.scene.control.Alert
import koma.IOFailure
import koma.koma_app.AppStore
import koma.matrix.UserId
import koma.matrix.UserPassword
import koma.matrix.login
import koma.matrix.user.identity.UserId_new
import koma.util.onFailure
import koma.util.onSuccess
import koma.util.testFailure
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.javafx.JavaFx
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import link.continuum.desktop.action.startChat
import link.continuum.database.KDataStore
import link.continuum.database.models.getToken
import link.continuum.database.models.saveServerAddr
import link.continuum.database.models.saveToken
import link.continuum.desktop.util.gui.alert
import mu.KotlinLogging
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import org.h2.mvstore.MVMap

/**
 * when the login button is clicked
 * accept text of text fields as parameters
 */
suspend fun onClickLogin(httpClient: OkHttpClient,
                         appData: AppStore,
                         userid: UserId, password: String, server: String,
                         keyValueMap: MVMap<String, String>
) {
    val url = HttpUrl.parse(server)
    if (url == null) {
        alert(Alert.AlertType.ERROR, "Invalid server url",
                "$server not parsed")
        return
    }
    val data = appData.database
    saveServerAddr(data, userid.server, server)
    val token = if (!password.isBlank()) {
        getTokenWithPassword(userid, password, httpClient, data, server)
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
    keyValueMap["active-account"] = userid.str
    token ?: return
    withContext(Dispatchers.Main) {
        startChat(httpClient,
                userid, token, url,
                keyValueMap,
                appData)
    }
}

/**
 * get token from server
 * saves the token to disk
 */
private suspend fun getTokenWithPassword(userid: UserId, password: String, httpClient: OkHttpClient,
                                         data: KDataStore,
                                         server: String): String? {
    val (it, ex, result) = login(UserPassword(user = userid.user, password = password), server, httpClient)
    if (!result.testFailure(it, ex)) {
        val u = it.user_id
        val t = it.access_token
        saveToken(data, u, t)
        return t
    } else {
        val mes = ex.toString()
        System.err.println(mes)
        val message = if (ex is IOFailure && ex.throwable is JsonEncodingException) {
            "Does ${server} have a valid JSON API?"
        } else mes
        GlobalScope.launch(Dispatchers.JavaFx) {
            alert(Alert.AlertType.ERROR, "Login Fail with Error",
                    message)
        }
    }
    return null
}
