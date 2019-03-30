package koma.gui.view.window.chatroom.messaging.reading.display.room_event.m_message.content

import com.github.kittinunf.result.success
import de.jensd.fx.glyphs.materialicons.MaterialIcon
import de.jensd.fx.glyphs.materialicons.utils.MaterialIconFactory
import javafx.scene.control.MenuItem
import javafx.scene.input.Clipboard
import javafx.scene.layout.StackPane
import koma.gui.dialog.file.save.downloadFileAs
import koma.gui.view.window.chatroom.messaging.reading.display.ViewNode
import koma.gui.view.window.chatroom.messaging.reading.display.room_event.m_message.common.ImageElement
import koma.koma_app.appState
import koma.matrix.event.room_message.chat.ImageMessage
import koma.network.media.MHUrl
import koma.storage.persistence.settings.AppSettings
import koma.util.result.ok
import tornadofx.*

private val settings: AppSettings = appState.store.settings

class MImageViewNode(val content: ImageMessage): ViewNode {
    override val node = StackPane()
    override val menuItems: List<MenuItem>
    private val server = appState.serverConf

    init {
        val url = MHUrl.fromStr(content.url).ok()
        val cnode = if (url != null) {
            menuItems = createMenuItems(url, content.body)
            ImageElement(url).node
        } else {
            menuItems = listOf()
            val s = settings.scale_em(1f)
            MaterialIconFactory.get().createIcon(MaterialIcon.BROKEN_IMAGE,
                    s)
        }
        node.add(cnode)
        node.tooltip(content.body)
    }

    private fun createMenuItems(url: MHUrl, filename: String): List<MenuItem> {
        val tm = MenuItem("Save Image")
        tm.action {
            url.toHttpUrl(server).success {
                downloadFileAs(it, filename = filename, title = "Save Image As")
            }
        }

        val copyUrl = MenuItem("Copy Image Address")
        copyUrl.action { Clipboard.getSystemClipboard().putString(
                url.toHttpUrl(server).fold({h -> h.toString()}, { _ -> url.toString()})
        ) }

        return listOf(tm, copyUrl)
    }
}


