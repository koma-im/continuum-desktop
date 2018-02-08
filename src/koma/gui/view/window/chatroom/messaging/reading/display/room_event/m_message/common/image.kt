package koma.gui.view.window.chatroom.messaging.reading.display.room_event.m_message.common

import javafx.scene.control.MenuItem
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.layout.StackPane
import koma.gui.dialog.file.save.downloadFileAs
import koma.gui.view.window.chatroom.messaging.reading.display.ViewNode
import koma.network.media.getResponse
import koma.storage.config.settings.AppSettings
import kotlinx.coroutines.experimental.launch
import okhttp3.HttpUrl
import tornadofx.*

class ImageElement(val url: HttpUrl): ViewNode {
    override val node = StackPane()
    override val menuItems: List<MenuItem>

    init {
        val imageView = ImageView()
        node.add(imageView)

        val scale = AppSettings.settings.scaling
        val imageSize = 200.0 * scale

        menuItems = menuItems()

        launch {
            val res = getResponse(url) ?: return@launch
            val image = Image(res.byteStream(), imageSize, imageSize, true, true)
            image.widthProperty()
            imageView.image = image
        }
    }

    private fun menuItems(): List<MenuItem> {
        val tm = MenuItem("Save Image")
        tm.action {
            downloadFileAs(url, title = "Save Image As")
        }
        return listOf(tm)
    }
}


