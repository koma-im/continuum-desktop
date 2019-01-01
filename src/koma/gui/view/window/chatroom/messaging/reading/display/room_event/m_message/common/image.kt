package koma.gui.view.window.chatroom.messaging.reading.display.room_event.m_message.common

import com.github.kittinunf.result.success
import javafx.beans.property.SimpleObjectProperty
import javafx.scene.control.MenuItem
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.input.MouseButton
import javafx.scene.layout.StackPane
import koma.gui.dialog.file.save.downloadFileAs
import koma.gui.view.window.chatroom.messaging.reading.display.ViewNode
import koma.koma_app.appData
import koma.koma_app.appState
import koma.network.media.MHUrl
import koma.network.media.downloadMedia
import koma.util.result.ok
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.javafx.JavaFx
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import tornadofx.*

class ImageElement(val url: MHUrl): ViewNode {
    override val node = StackPane()
    override val menuItems: List<MenuItem>
    private val server = appState.serverConf

    init {
        val imageProperty = SimpleObjectProperty<Image>()
        val imageView = ImageView()
        node.add(imageView)
        node.setOnMouseClicked { event ->
            if (event.button == MouseButton.PRIMARY) {
                viewBiggerPicture(imageProperty, url)
            }
        }

        val scale = appData.settings.scaling
        val imageSize = 200.0 * scale

        menuItems = menuItems()

        GlobalScope.launch {
            val res = appState.koma.downloadMedia(url, server).ok() ?: return@launch
            val image = Image(res.inputStream())
            if (image.width > imageSize) {
                imageView.fitHeight = imageSize
                imageView.fitWidth = imageSize
                imageView.isPreserveRatio = true
                imageView.isSmooth = true
            }
            imageView.image = image
            withContext(Dispatchers.JavaFx) {
                imageProperty.set(image)
            }
        }
    }

    private fun menuItems(): List<MenuItem> {
        val tm = MenuItem("Save Image")
        tm.action {
            url.toHttpUrl(server).success {
                downloadFileAs(it, title = "Save Image As")
            }
        }
        return listOf(tm)
    }
}

/**
 * Display an overlay showing a picture occupying most of the entire window
 */
fun viewBiggerPicture(
        image: SimpleObjectProperty<Image>,
        url: MHUrl
) {
    val owner = FX.primaryStage.scene.root
    val win = InternalWindow(
            icon = null,
            modal = true,
            escapeClosesWindow = true,
            closeButton = true,
            overlayPaint = c("#000", 0.4))
    win.open(view = BiggerPictureView(image, url), owner = owner)
}

private class BiggerPictureView(
        image: SimpleObjectProperty<Image>,
        url: MHUrl
): View() {
    override val root = StackPane()
    init {
        title = url.toString()

        with(root) {
            imageview(image) {
                fitHeight = FX.primaryStage.height * 0.9
                fitWidth = FX.primaryStage.width * 0.9
                isPreserveRatio = true
                isSmooth = true
            }
        }
    }
}
