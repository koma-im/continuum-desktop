package koma.gui.view.window.chatroom.messaging.reading.display.room_event.m_message.content

import javafx.scene.control.Alert
import javafx.scene.control.MenuItem
import javafx.scene.control.Tooltip
import javafx.scene.input.Clipboard
import javafx.scene.input.ClipboardContent
import koma.Koma
import koma.Server
import koma.gui.dialog.file.save.downloadFileAs
import koma.gui.view.window.chatroom.messaging.reading.display.ViewNode
import koma.gui.view.window.chatroom.messaging.reading.display.room_event.m_message.common.ImageElement
import koma.matrix.event.room_message.chat.ImageMessage
import koma.network.media.parseMxc
import kotlinx.coroutines.ExperimentalCoroutinesApi
import link.continuum.desktop.gui.action
import mu.KotlinLogging
import okhttp3.HttpUrl

private val logger = KotlinLogging.logger {}

@ExperimentalCoroutinesApi
class MImageViewNode(km: Koma): ViewNode {
    override val menuItems: List<MenuItem>  = createMenuItems()

    private var url: HttpUrl? = null
    private var filename: String? =null
    private var image = ImageElement(km)
    override val node = image.node
    private val tooltip = Tooltip()
    init {
        Tooltip.install(node, tooltip)
    }
    fun update(content: ImageMessage, server: Server) {
        val u = content.url.parseMxc()
        if (u == null) {
            logger.warn { "url ${content.url} not parsed" }
        } else {
            url = server.mxcToHttp(u)
            image.update(u, server)
        }
        tooltip.text =content.body
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
        copyUrl.action {
            Clipboard.getSystemClipboard().setContent(ClipboardContent().apply { putString(url) })
        }

        return listOf(tm, copyUrl)
    }
}


