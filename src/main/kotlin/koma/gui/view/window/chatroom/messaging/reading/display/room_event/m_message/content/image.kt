package koma.gui.view.window.chatroom.messaging.reading.display.room_event.m_message.content

import javafx.scene.control.Alert
import javafx.scene.control.MenuItem
import javafx.scene.input.Clipboard
import koma.Koma
import koma.Server
import koma.gui.dialog.file.save.downloadFileAs
import koma.gui.view.window.chatroom.messaging.reading.display.ViewNode
import koma.gui.view.window.chatroom.messaging.reading.display.room_event.m_message.common.ImageElement
import koma.matrix.event.room_message.chat.ImageMessage
import koma.network.media.parseMxc
import kotlinx.coroutines.ExperimentalCoroutinesApi
import mu.KotlinLogging
import okhttp3.HttpUrl
import tornadofx.*

private val logger = KotlinLogging.logger {}

@ExperimentalCoroutinesApi
class MImageViewNode(private val server: Server
                     ): ViewNode {
    override val menuItems: List<MenuItem>  = createMenuItems()

    private var url: HttpUrl? = null
    private var filename: String? =null
    private var image = ImageElement(server.km)
    override val node = image.node

    fun update(content: ImageMessage, server: Server) {
        val u = content.url.parseMxc()
        if (u == null) {
            logger.warn { "url ${content.url} not parsed" }
        } else {
            url = server.mxcToHttp(u)
            image.update(u, server)
        }
        node.tooltip(content.body)
    }

    private fun createMenuItems(): List<MenuItem> {
        val tm = MenuItem("Save Image")
        tm.action {
            val u = url
            if (u == null) {
                link.continuum.desktop.util.gui.alert(Alert.AlertType.ERROR,
                        "No url to download")
            } else {
                downloadFileAs(u, filename = filename ?: "image", title = "Save Image As")
            }
        }

        val copyUrl = MenuItem("Copy Image Address")
        copyUrl.action { Clipboard.getSystemClipboard().putString(url.toString()) }

        return listOf(tm, copyUrl)
    }
}


