package koma.gui.view.window.chatroom.messaging.reading.display.room_event.m_message.common

import javafx.scene.control.Alert
import javafx.scene.control.MenuItem
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.input.MouseButton
import javafx.scene.layout.StackPane
import koma.gui.dialog.file.save.downloadFileAs
import koma.gui.view.window.chatroom.messaging.reading.display.ViewNode
import koma.koma_app.appState
import kotlinx.coroutines.*
import kotlinx.coroutines.javafx.JavaFx
import link.continuum.desktop.util.getOr
import link.continuum.desktop.util.http.downloadHttp
import mu.KotlinLogging
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import tornadofx.*

private val logger = KotlinLogging.logger {}

@ExperimentalCoroutinesApi
class ImageElement(
        private val client: OkHttpClient,
        private val imageSize: Double = 200.0 * appState.store.settings.scaling
): ViewNode {
    override val node = StackPane()
    override val menuItems: List<MenuItem>

    private var imageView: ImageView
    private var title: String = ""
    private var url: HttpUrl? = null
    private var job: Job? = null

    fun update(url: HttpUrl, mxcUrl: String? = null) {
        this.url = url
        imageView.image = null
        job?.cancel()

        title = mxcUrl ?: url.toString()
        job = GlobalScope.launch {
            val res = downloadHttp(url, client) getOr {
                return@launch
            }
            val image = Image(res.inputStream())
            if (image.width > imageSize) {
                imageView.fitHeight = imageSize
                imageView.fitWidth = imageSize
                imageView.isPreserveRatio = true
                imageView.isSmooth = true
            }
            withContext(Dispatchers.JavaFx) {
                imageView.image = image
            }
        }
    }
    init {
        imageView = ImageView()
        node.add(imageView)
        node.setOnMouseClicked { event ->
            if (event.button == MouseButton.PRIMARY) {
                viewBiggerPicture()
            }
        }


        menuItems = menuItems()
    }

    private fun menuItems(): List<MenuItem> {
        val tm = MenuItem("Save Image")
        tm.setOnAction {
            val u = url
            if (u == null) {
                link.continuum.desktop.util.gui.alert(
                        Alert.AlertType.ERROR,
                        "Can't download",
                        "url is null"
                )
            } else {
                downloadFileAs(u, title = "Save Image As")
            }
        }
        return listOf(tm)
    }


    /**
     * Display an overlay showing a picture occupying most of the entire window
     */
    fun viewBiggerPicture() {
        val owner = FX.primaryStage.scene.root
        biggerView.update(title, imageView.image)
        imageWin.open(view = biggerView, owner = owner)
    }
}


private val imageWin by lazy {
    InternalWindow(
            icon = null,
            modal = true,
            escapeClosesWindow = true,
            closeButton = true,
            overlayPaint = c("#000", 0.4))
}

private val biggerView by lazy {
    BiggerPictureView()
}

private class BiggerPictureView(): View() {
    override val root = StackPane()
    private var imageView: ImageView
    init {
        with(root) {
            imageView = imageview() {
                fitHeight = FX.primaryStage.height * 0.9
                fitWidth = FX.primaryStage.width * 0.9
                isPreserveRatio = true
                isSmooth = true
            }
        }
    }

    fun update(title: String, image: Image?) {
        imageView.image = image
        this.title = title
    }
}