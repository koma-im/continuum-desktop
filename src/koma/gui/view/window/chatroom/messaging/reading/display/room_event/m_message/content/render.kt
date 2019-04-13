package koma.gui.view.window.chatroom.messaging.reading.display.room_event.m_message.content

import koma.gui.view.window.chatroom.messaging.reading.display.ViewNode
import koma.koma_app.appState
import koma.matrix.event.room_message.MRoomMessage
import koma.matrix.event.room_message.chat.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import link.continuum.desktop.gui.list.user.UserDataStore
import okhttp3.HttpUrl

@ExperimentalCoroutinesApi
class MessageView(
        private val userDataStore: UserDataStore,
        private val server: HttpUrl) {
    var node: ViewNode? = null

    private val emote by lazy { MEmoteViewNode(userDataStore) }
    private val notice by lazy { MNoticeViewNode() }
    private val text by lazy { MTextViewNode() }
    private val image by lazy { MImageViewNode(server, appState.koma.http.client) }
    fun update(message: MRoomMessage) {
        val content = message.content
        node = when(content) {
            is TextMessage ->text.apply {
                update(content)
            }
            is NoticeMessage -> notice.apply{
                update(content)
            }
            is EmoteMessage -> emote.apply{
                update(content, message.sender)
            }
            is ImageMessage -> image.apply { update(content) }
            is FileMessage -> {
                MFileViewNode(content, server)
            }
            else -> null
        }
    }
}
