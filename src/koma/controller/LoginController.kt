package controller

import javafx.scene.control.Alert
import koma.gui.setSaneStageSize
import koma.storage.config.profile.Profile
import koma.storage.config.profile.saveLastUsed
import koma.storage.config.server.ServerConf
import koma_app.appState
import matrix.ApiClient
import matrix.UserPassword
import matrix.login
import matrix.register
import rx.javafx.kt.observeOnFx
import rx.schedulers.Schedulers
import tornadofx.*
import view.ChatView
import view.RootLayoutView

data class LoginData(
        val profile: Profile?,
        val serverConf: ServerConf
)

class LoginController: Controller() {
    init {
        guiEvents.loginRequests.toObservable().observeOn(Schedulers.io())
                .map {
                    val authed = if (it.password == null) {
                        Profile.new(it.user)
                    } else {
                        login(
                                UserPassword(user = it.user.toString(), password = it.password),
                                it.serverConf
                                )
                    }
                    LoginData(authed, it.serverConf)
                }
                .observeOnFx()
                .subscribe { postLogin(it) }
        guiEvents.registerRequests.toObservable().observeOn(Schedulers.io())
                .map {
                    val user = register(it.usereg, it.serverConf)?.let {
                        Profile(it.user_id, it.access_token)
                    }
                    LoginData(
                            user,
                            it.serverConf
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
        saveLastUsed(userid)

        val apiClient = ApiClient(authed, target.serverConf)
        appState.apiClient = apiClient
        appState.serverConf = target.serverConf

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

        chatctrl.start()
    }
}
