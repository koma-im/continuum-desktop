package koma.gui.view.window.auth.login

import controller.ChatController
import koma.Koma
import koma.matrix.UserId
import koma.storage.config.profile.Profile
import koma.storage.config.profile.saveLastUsed
import koma.storage.config.server.ServerConf
import koma_app.appState
import matrix.ApiClient
import tornadofx.*
import view.ChatView
import view.RootLayoutView

/**
 * show the chat window after login is done
 */
fun Koma.startChat(authedUser: Profile, serverConf: ServerConf) {
    val userid = authedUser.userId
    saveLastUsed(userid)

    val apiClient = ApiClient(authedUser, serverConf, appState.koma)
    appState.apiClient = apiClient
    appState.serverConf = serverConf

    val chatview = ChatView(authedUser)
    val chatctrl = ChatController(apiClient)
    appState.chatController = chatctrl
    val rootView = RootLayoutView(chatctrl)
    rootView.root.center = chatview.root
    val stage = FX.primaryStage
    stage.scene.root = rootView.root

    chatctrl.start()
}

fun Koma.startChatWithIdToken(userId: UserId, token: String, serverConf: ServerConf) {
    val p = Profile(userId, token)
    startChat(p, serverConf)
}
