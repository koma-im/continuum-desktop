package controller

import javafx.scene.control.Alert
import koma.gui.setSaneStageSize
import koma.storage.config.profile.Profile
import koma.storage.config.profile.saveLastUsed
import koma.storage.config.server.ServerConf
import koma_app.appState
import matrix.ApiClient
import matrix.register
import rx.javafx.kt.observeOnFx
import rx.schedulers.Schedulers
import tornadofx.*
import view.ChatView
import view.RootLayoutView

private class LoginData(
        val profile: Profile?,
        val serverConf: ServerConf
)

class LoginController: Controller() {
    init {
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
                .subscribe {
                    if (it.profile == null) {
                        alert(Alert.AlertType.ERROR, "Register Fail")
                    } else {
                        postLogin(it.profile, it.serverConf)
                    }
                }
    }

    fun postLogin(authedUser: Profile, serverConf: ServerConf) {
        val userid = authedUser.userId
        saveLastUsed(userid)

        val apiClient = ApiClient(authedUser, serverConf)
        appState.apiClient = apiClient
        appState.serverConf = serverConf

        val chatview = ChatView(authedUser)
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
