package koma.gui.view.window.chatroom.messaging.reading.display.room_event.m_message.content

import koma.Server
import koma.gui.view.window.chatroom.messaging.reading.display.ViewNode
import koma.matrix.UserId
import koma.matrix.event.room_message.MRoomMessage
import koma.matrix.event.room_message.chat.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import link.continuum.desktop.gui.list.user.UserDataStore
import link.continuum.desktop.util.http.MediaServer
import okhttp3.OkHttpClient

@ExperimentalCoroutinesApi
class MessageView(
        private val userDataStore: UserDataStore
) {
    var node: ViewNode? = null

    private val emote by lazy { MEmoteViewNode(userDataStore) }
    private val notice by lazy { MNoticeViewNode(userDataStore.data) }
    private val text by lazy { MTextViewNode(userDataStore.data) }
    private val image by lazy { MImageViewNode() }
    fun update(message: MRoomMessage, server: Server) {
        val content = message.content
        update(content, server, message.sender)
    }
    fun update(content: M_Message?, server: Server, sender: UserId) {
        node = when(content) {
            is TextMessage ->text.apply {
                update(content, server)
            }
            is NoticeMessage -> notice.apply{
                update(content, server)
            }
            is EmoteMessage -> emote.apply{
                update(content, sender, server)
            }
            is ImageMessage -> image.apply { update(content, server) }
            is FileMessage -> {
                MFileViewNode(content, server)
            }
            else -> null
        }
    }
}
