package koma.gui.view.window.chatroom.messaging.reading.display.room_event.m_message.content

import de.jensd.fx.glyphs.materialicons.MaterialIcon
import de.jensd.fx.glyphs.materialicons.utils.MaterialIconFactory
import javafx.scene.control.MenuItem
import javafx.scene.layout.StackPane
import koma.gui.dialog.file.save.downloadFileAs
import koma.gui.view.window.chatroom.messaging.reading.display.ViewNode
import koma.gui.view.window.chatroom.messaging.reading.display.room_event.m_message.common.ImageElement
import koma.matrix.event.room_message.chat.ImageMessage
import koma.network.matrix.media.makeAnyUrlHttp
import koma.storage.config.settings.AppSettings
import tornadofx.*

class MImageViewNode(val content: ImageMessage): ViewNode {
    override val node = StackPane()
    override val menuItems: List<MenuItem>

    init {
        val url = makeAnyUrlHttp(content.url)
        val cnode = if (url != null) {
            val tm = MenuItem("Save Image")
            tm.action { downloadFileAs(url, filename = content.body, title = "Save Image As") }
            menuItems = listOf(tm)
            ImageElement(url).node
        } else {
            menuItems = listOf()
            MaterialIconFactory.get().createIcon(MaterialIcon.BROKEN_IMAGE, AppSettings.scale_em(1f))
        }
        node.add(cnode)
        node.tooltip(content.body)
    }
}

