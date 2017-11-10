package controller

import javafx.scene.control.Alert
import koma.gui.setSaneStageSize
import koma_app.appState
import matrix.*
import rx.javafx.kt.observeOnFx
import rx.schedulers.Schedulers
import tornadofx.*
import util.getToken
import util.saveLastUsed
import view.ChatView
import view.RootLayoutView
import java.net.Proxy

data class LoginData(
        val serverAddr: String,
        val authed: AuthedUser?,
        val proxy: Proxy
)

class LoginController: Controller() {
    init {
        guiEvents.loginRequests.toObservable().observeOn(Schedulers.io())
                .map {
                    val authed = if (it.password == null) {
                        val tol = getToken(it.user)
                        tol
                    } else {
                        login(
                                it.server,
                                UserPassword(user = it.user.toString(), password = it.password),
                                it.proxy)
                    }
                    LoginData(it.server, authed, it.proxy)
                }
                .observeOnFx()
                .subscribe { postLogin(it) }
        guiEvents.registerRequests.toObservable().observeOn(Schedulers.io())
                .map {
                    val user = register(it.server, it.usereg, it.proxy)?.let {
                        AuthedUser(it.access_token, it.user_id)
                    }
                    LoginData(
                            it.server,
                            user,
                            it.proxy
                    )
                }
                .observeOnFx()
                .subscribe { postLogin(it) }
    }

    private fun postLogin(target: LoginData) {
        val authed = target.authed
        val userid = authed?.user_id
        if (authed == null || userid == null) {
            alert(Alert.AlertType.WARNING, "Invalid user-id/password")
            return
        }
        val server = target.serverAddr
        saveLastUsed(userid, server)

        val serverUrl = server.trimEnd('/') + "/"

        val apiClient = ApiClient(serverUrl, authed, target.proxy)
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
