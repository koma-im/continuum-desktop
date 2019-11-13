package koma.gui.view.window.chatroom.messaging.reading.display.room_event.m_message.content

import javafx.scene.control.Alert
import javafx.scene.control.MenuItem
import javafx.scene.control.Tooltip
import javafx.scene.input.Clipboard
import javafx.scene.input.ClipboardContent
import koma.Server
import koma.gui.dialog.file.save.downloadFileAs
import koma.gui.view.window.chatroom.messaging.reading.display.ViewNode
import koma.gui.view.window.chatroom.messaging.reading.display.room_event.m_message.common.ImageElement
import koma.matrix.event.room_message.chat.ImageMessage
import koma.network.media.parseMxc
import kotlinx.coroutines.ExperimentalCoroutinesApi
import link.continuum.desktop.gui.action
import link.continuum.desktop.util.gui.alert
import link.continuum.desktop.util.http.MediaServer
import mu.KotlinLogging
import okhttp3.HttpUrl
import okhttp3.OkHttpClient

private val logger = KotlinLogging.logger {}

@ExperimentalCoroutinesApi
class MImageViewNode(): ViewNode {
    override val menuItems: List<MenuItem>  = createMenuItems()

    private var url: HttpUrl? = null
    private var filename: String? =null
    private var image = ImageElement()
    override val node = image.node
    private val tooltip = Tooltip()
    private var server: MediaServer? = null
    init {
        Tooltip.install(node, tooltip)
    }
    fun update(content: ImageMessage, server: Server) {
        this.server = server
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
                alert(Alert.AlertType.ERROR,
                        "No url to download")
                return@action
            }
            val httpClient = server?.httpClient ?: kotlin.run {
                alert(Alert.AlertType.ERROR,
                        "No server to download")
                return@action
            }
            downloadFileAs(u, filename = filename ?: "image", title = "Save Image As", httpClient = httpClient)
        }

        val copyUrl = MenuItem("Copy Image Address")
        copyUrl.action {
            Clipboard.getSystemClipboard().setContent(ClipboardContent().apply { putString(url) })
        }

        return listOf(tm, copyUrl)
    }
}


