package koma.gui.view.window.chatroom.messaging.reading.display.room_event.m_message.common

import javafx.scene.control.MenuItem
import javafx.scene.image.ImageView
import javafx.scene.layout.StackPane
import koma.gui.dialog.file.save.downloadFileAs
import koma.gui.view.window.chatroom.messaging.reading.display.ViewNode
import koma.network.media.downloadImageHttp
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
        imageView.isPreserveRatio = true
        val scale = AppSettings.settings.scaling
        imageView.fitWidth = 200.0 * scale
        imageView.fitHeight = 200.0 * scale
        imageView.isSmooth = true

        menuItems = menuItems()

        launch {
            val image = downloadImageHttp(url)
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


