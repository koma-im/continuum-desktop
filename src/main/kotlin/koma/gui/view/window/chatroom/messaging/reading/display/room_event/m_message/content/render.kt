package koma.gui.view.window.chatroom.messaging.reading.display.room_event.m_message.content

import koma.Server
import koma.gui.view.window.chatroom.messaging.reading.display.ViewNode
import koma.matrix.event.room_message.MRoomMessage
import koma.matrix.event.room_message.chat.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import link.continuum.desktop.gui.list.user.UserDataStore
import okhttp3.OkHttpClient

@ExperimentalCoroutinesApi
class MessageView(
        private val userDataStore: UserDataStore,
        private val km: OkHttpClient
) {
    var node: ViewNode? = null

    private val emote by lazy { MEmoteViewNode(userDataStore, km) }
    private val notice by lazy { MNoticeViewNode(km) }
    private val text by lazy { MTextViewNode(km) }
    private val image by lazy { MImageViewNode(km) }
    fun update(message: MRoomMessage, server: Server) {
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
            is ImageMessage -> image.apply { update(content, server) }
            is FileMessage -> {
                MFileViewNode(content, server)
            }
            else -> null
        }
    }
}
