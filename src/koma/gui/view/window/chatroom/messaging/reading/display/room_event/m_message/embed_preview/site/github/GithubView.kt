package koma.gui.view.window.chatroom.messaging.reading.display.room_event.m_message.embed_preview.site.github

import javafx.scene.control.MenuItem
import javafx.scene.text.TextFlow
import koma.gui.view.window.chatroom.messaging.reading.display.ViewNode
import tornadofx.*

class GithubView(link: String): ViewNode {
    override val node = TextFlow()
    override val menuItems: List<MenuItem>

    init {
        with(node) {
            text("Github ")
            text("preview coming soon")
        }

        val mi = MenuItem("sample menu item")
        menuItems = listOf(mi)
    }
}
