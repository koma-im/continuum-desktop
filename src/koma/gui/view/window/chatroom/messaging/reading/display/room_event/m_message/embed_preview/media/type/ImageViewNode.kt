package koma.gui.view.window.chatroom.messaging.reading.display.room_event.m_message.embed_preview.media.type

import javafx.scene.control.MenuItem
import javafx.scene.image.ImageView
import koma.gui.view.window.chatroom.messaging.reading.display.ViewNode
import tornadofx.*

class ImageViewNode(link: String): ViewNode {
    override val node: ImageView
    override val menuItems: List<MenuItem>

    init {
        node = ImageView()
        val menusave = MenuItem("Save image")
        menusave.action {  }
        menuItems = listOf(menusave)
    }
}
