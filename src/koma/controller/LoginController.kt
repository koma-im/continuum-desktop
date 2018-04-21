package controller

import koma.gui.setSaneStageSize
import koma.storage.config.profile.Profile
import koma.storage.config.profile.saveLastUsed
import koma.storage.config.server.ServerConf
import koma_app.appState
import matrix.ApiClient
import tornadofx.*
import view.ChatView
import view.RootLayoutView

class LoginController: Controller() {
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
