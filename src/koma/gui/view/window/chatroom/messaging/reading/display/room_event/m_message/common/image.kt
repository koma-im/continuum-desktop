package koma.gui.view.window.chatroom.messaging.reading.display.room_event.m_message.common

import com.github.kittinunf.result.success
import javafx.scene.control.MenuItem
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.layout.StackPane
import koma.gui.dialog.file.save.downloadFileAs
import koma.gui.view.window.chatroom.messaging.reading.display.ViewNode
import koma.network.media.MHUrl
import koma.network.media.downloadMedia
import koma.util.result.ok
import koma_app.appState
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import tornadofx.*

class ImageElement(val url: MHUrl): ViewNode {
    override val node = StackPane()
    override val menuItems: List<MenuItem>

    init {
        val imageView = ImageView()
        node.add(imageView)

        val scale = appState.koma.appSettings.settings.scaling
        val imageSize = 200.0 * scale

        menuItems = menuItems()

        GlobalScope.launch {
            val res = appState.koma.downloadMedia(url).ok() ?: return@launch
            val image = Image(res.inputStream())
            if (image.width > imageSize) {
                imageView.fitHeight = imageSize
                imageView.fitWidth = imageSize
                imageView.isPreserveRatio = true
                imageView.isSmooth = true
            }
            imageView.image = image
        }
    }

    private fun menuItems(): List<MenuItem> {
        val tm = MenuItem("Save Image")
        tm.action {
            url.toHttpUrl().success {
                downloadFileAs(it, title = "Save Image As")
            }
        }
        return listOf(tm)
    }
}


