package controller

import javafx.scene.control.Alert
import koma.gui.setSaneStageSize
import koma_app.appState
import matrix.*
import rx.javafx.kt.observeOnFx
import rx.lang.kotlin.filterNotNull
import rx.schedulers.Schedulers
import tornadofx.Controller
import tornadofx.FX
import tornadofx.alert
import util.getToken
import util.saveLastUsed
import view.ChatView
import view.RootLayoutView

/**
 * Created by developer on 2017/6/21.
 */
class LoginController: Controller() {
    init {
        guiEvents.loginRequests.toObservable().observeOn(Schedulers.io())
                .map {
                    val authed = if (it.password == null) {
                        val tol = getToken(it.user)
                        tol
                    } else {
                        login(it.server, UserPassword(user = it.user.toString(), password = it.password))
                    }
                    Pair(it.server, authed)
                }
                .observeOnFx()
                .subscribe { postLogin(it) }
        guiEvents.registerRequests.toObservable().observeOn(Schedulers.io())
                .map {
                    Pair(it.server, register(it.server, it.usereg))
                }.filterNotNull()
                .map {
                    val registered = it.second
                    Pair(it.first,
                            if (registered != null)
                                AuthedUser(registered.access_token, registered.user_id)
                            else
                                null
                    )
                }
                .observeOnFx()
                .subscribe { postLogin(it) }
    }

    private fun postLogin(target: Pair<String, AuthedUser?>) {
        val authed = target.second
        val userid = authed?.user_id
        if (authed == null || userid == null) {
            alert(Alert.AlertType.WARNING, "Invalid user-id/password")
            return
        }
        val server = target.first
        saveLastUsed(userid, server)

        val serverUrl = server.trimEnd('/') + "/"

        val apiClient = ApiClient(serverUrl, authed)
        appState.apiClient = apiClient

        val chatview = ChatView()
        val chatctrl = ChatController(apiClient)
        appState.chatController = chatctrl
        val rootView = RootLayoutView(chatctrl)
        rootView.root.center = chatview.root
        val stage = FX.primaryStage
        stage.scene.root = rootView.root
        setSaneStageSize(stage)
        stage.hide()
        stage.show()
    }
}
