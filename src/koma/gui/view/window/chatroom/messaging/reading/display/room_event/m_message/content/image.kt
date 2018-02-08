package koma.gui.view.window.chatroom.messaging.reading.display.room_event.m_message.content

import javafx.beans.property.SimpleBooleanProperty
import javafx.scene.control.MenuItem
import javafx.scene.image.ImageView
import koma.gui.dialog.file.save.downloadFileAs
import koma.gui.view.window.chatroom.messaging.reading.display.ViewNode
import koma.matrix.event.room_message.chat.ImageMessage
import koma.network.matrix.media.makeAnyUrlHttp
import koma.network.media.downloadImageHttp
import koma.storage.config.settings.AppSettings
import kotlinx.coroutines.experimental.launch
import okhttp3.HttpUrl
import tornadofx.*

class MImageViewNode(val content: ImageMessage): ViewNode {
    override val node = ImageView()
    override val menuItems: List<MenuItem>

    private val url: HttpUrl?
    private val imageAvailable = SimpleBooleanProperty(false)

    init {
        url = makeAnyUrlHttp(content.url)

        val imageView = node
        imageView.isPreserveRatio = true
        imageView.tooltip(content.body)
        val scale = AppSettings.settings.scaling
        imageView.fitWidth = 320.0 * scale
        imageView.fitHeight = 320.0 * scale
        imageView.isSmooth = true

        menuItems = menuItems()

        launch {
            url?.let {  downloadImageHttp(it)}
                    ?.let {
                        imageAvailable.set(true)
                        imageView.image = it
                    }
        }
    }

    private fun menuItems(): List<MenuItem> {
        val tm = MenuItem("Save Image")
        tm.action {
            url?.let {
                downloadFileAs(url, filename = content.body, title = "Save Image As")
            }
        }

        return listOf(tm)
    }
}


