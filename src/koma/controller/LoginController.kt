package controller

import javafx.scene.control.Alert
import koma.gui.setSaneStageSize
import koma.storage.config.profile.Profile
import koma_app.appState
import matrix.ApiClient
import matrix.UserPassword
import matrix.login
import matrix.register
import rx.javafx.kt.observeOnFx
import rx.schedulers.Schedulers
import tornadofx.*
import util.saveLastUsed
import view.ChatView
import view.RootLayoutView
import java.net.Proxy

data class LoginData(
        val serverAddr: String,
        val profile: Profile?,
        val proxy: Proxy
)

class LoginController: Controller() {
    init {
        guiEvents.loginRequests.toObservable().observeOn(Schedulers.io())
                .map {
                    val authed = if (it.password == null) {
                        Profile.new(it.user)
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
                        Profile(it.user_id, it.access_token)
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
        val authed = target.profile
        val userid = authed?.userId
        if (authed == null || userid == null) {
            alert(Alert.AlertType.WARNING, "Invalid user-id/password")
            return
        }
        val server = target.serverAddr
        saveLastUsed(userid, server)

        val serverUrl = server.trimEnd('/') + "/"

        val apiClient = ApiClient(serverUrl, authed, target.proxy)
        appState.apiClient = apiClient

        val chatview = ChatView(authed)
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
