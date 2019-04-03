package koma.gui.view.window.chatroom.messaging.reading.display.room_event.m_message.common

import javafx.beans.property.SimpleObjectProperty
import javafx.scene.control.MenuItem
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.input.MouseButton
import javafx.scene.layout.StackPane
import koma.gui.dialog.file.save.downloadFileAs
import koma.gui.view.window.chatroom.messaging.reading.display.ViewNode
import koma.koma_app.appState
import koma.storage.persistence.settings.AppSettings
import koma.util.result.ok
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.javafx.JavaFx
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import link.continuum.desktop.util.http.downloadHttp
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import tornadofx.*

private val settings: AppSettings = appState.store.settings

class ImageElement(
        val url: HttpUrl,
        private val client: OkHttpClient,
        val mxcUrl: String? = null
): ViewNode {
    override val node = StackPane()
    override val menuItems: List<MenuItem>

    init {
        val imageProperty = SimpleObjectProperty<Image>()
        val imageView = ImageView()
        node.add(imageView)
        node.setOnMouseClicked { event ->
            if (event.button == MouseButton.PRIMARY) {
                val title = mxcUrl ?: url.toString()
                viewBiggerPicture(imageProperty, title)
            }
        }

        val scale = settings.scaling
        val imageSize = 200.0 * scale

        menuItems = menuItems()

        GlobalScope.launch {
            val res = downloadHttp(url, client).ok() ?: return@launch
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
            downloadFileAs(url, title = "Save Image As")
        }
        return listOf(tm)
    }
}

/**
 * Display an overlay showing a picture occupying most of the entire window
 */
fun viewBiggerPicture(
        image: SimpleObjectProperty<Image>,
        title: String
) {
    val owner = FX.primaryStage.scene.root
    val win = InternalWindow(
            icon = null,
            modal = true,
            escapeClosesWindow = true,
            closeButton = true,
            overlayPaint = c("#000", 0.4))
    win.open(view = BiggerPictureView(image, title), owner = owner)
}

private class BiggerPictureView(
        image: SimpleObjectProperty<Image>,
        url: String
): View() {
    override val root = StackPane()
    init {
        title = url

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
