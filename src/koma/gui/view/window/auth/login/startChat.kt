package koma.gui.view.window.auth.login

import controller.ChatController
import koma.Koma
import koma.koma_app.appState
import koma.matrix.UserId
import koma.storage.config.profile.saveLastUsed
import koma.storage.config.server.ServerConf
import koma.storage.config.server.getAddress
import kotlinx.coroutines.ObsoleteCoroutinesApi
import okhttp3.HttpUrl
import tornadofx.*
import view.ChatView
import view.RootLayoutView

/**
 * show the chat window after login is done
 * updates the list of recently used accounts
 */
@ObsoleteCoroutinesApi
fun startChat(koma: Koma, userId: UserId, token: String, serverConf: ServerConf) {
    koma.saveLastUsed(userId)

    val url = serverConf.getAddress().let { HttpUrl.parse(it) }
    val apiClient  = koma.createApi(token, userId, url!!)
    appState.apiClient = apiClient
    appState.serverConf = serverConf

    val chatview = ChatView(userId)
    val chatctrl = ChatController(apiClient)
    appState.chatController = chatctrl
    val rootView = RootLayoutView(chatctrl)
    rootView.root.center = chatview.root
    val stage = FX.primaryStage
    stage.scene.root = rootView.root

    chatctrl.start()
}
