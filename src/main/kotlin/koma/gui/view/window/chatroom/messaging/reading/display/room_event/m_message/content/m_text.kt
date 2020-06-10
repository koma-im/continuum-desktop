package koma.gui.view.window.chatroom.messaging.reading.display.room_event.m_message.content

import javafx.scene.control.Label
import javafx.scene.control.MenuItem
import javafx.scene.text.Text
import javafx.scene.text.TextFlow
import koma.gui.view.window.chatroom.messaging.reading.display.ViewNode
import koma.gui.view.window.chatroom.messaging.reading.display.room_event.m_message.embed_preview.*
import koma.matrix.UserId
import koma.matrix.event.room_message.chat.EmoteMessage
import koma.matrix.event.room_message.chat.NoticeMessage
import koma.matrix.event.room_message.chat.TextMessage
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import link.continuum.desktop.gui.add
import link.continuum.desktop.gui.list.user.UserDataStore
import link.continuum.desktop.gui.message.richtext.parseRichXml
import link.continuum.desktop.observable.MutableObservable
import link.continuum.desktop.util.http.MediaServer

class MTextViewNode(private val data: UserDataStore): ViewNode {
    override val node = TextFlow()
    override val menuItems: List<MenuItem> = listOf()

    fun update(content: TextMessage, server: MediaServer) {
        node.children.clear()
        val richtext = content.formatted_body?.parseRichXml()
        if (richtext != null) {
            val nodes = richtext.flatMap {
                if (it !is PlainTextSegment) { listOf(it)
                } else { tokenize_string(it.text) }
            }.map { messageSliceView(it, server, data) }.toNodes()
            node.children.addAll(nodes)
        } else {
            node.addStringWithElements(content.body, server, data)
        }
    }

    fun updatePlainText(text: String, server: MediaServer) {
        node.children.clear()
        node.addStringWithElements(text, server, data)
    }
}

class MNoticeViewNode(private val data: UserDataStore): ViewNode {
    override val node = TextFlow()
    override val menuItems: List<MenuItem> = listOf()

    fun update(content: NoticeMessage, server: MediaServer) {
        node.children.clear()
        node.addStringWithElements(content.body, server, data)
    }
}


@ExperimentalCoroutinesApi
class MEmoteViewNode(
        private val userData: UserDataStore
): ViewNode {
    private val scope = MainScope()
    override val node = TextFlow()
    override val menuItems: List<MenuItem> = listOf()

    private val userLabel = Label()

    private val userIdFlow = MutableObservable<UserId>()

    fun update(content: EmoteMessage, sender: UserId, server: MediaServer) {
        userLabel.text = ""
        node.children.clear()
        userLabel.textFill = userData.getUserColor(sender)

        node.add(userLabel)
        node.add(Text(" "))
        node.addStringWithElements(content.body, server, userData)
    }

    init {
        userIdFlow
                .flow()
                .flatMapLatest {
            userData.getNameUpdates(it)
        }.onEach {
            userLabel.text = it
        }.launchIn(scope)
    }
}


