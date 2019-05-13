package link.continuum.desktop.gui.icon.avatar

import javafx.scene.image.ImageView
import javafx.scene.layout.StackPane
import javafx.scene.paint.Color
import koma.gui.element.icon.user.extract_key_chars
import koma.matrix.UserId
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.javafx.JavaFx
import kotlinx.coroutines.selects.select
import link.continuum.desktop.gui.list.user.UserDataStore
import mu.KotlinLogging
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import tornadofx.add
import tornadofx.booleanBinding
import tornadofx.removeWhen
import java.util.concurrent.atomic.AtomicInteger


private val logger = KotlinLogging.logger {}

@ExperimentalCoroutinesApi
class UrlAvatar(
        private val client: OkHttpClient,
        private val avatarSize: Double
) {
    val root = object :StackPane() {
        // roughly aligned with text vertically
        override fun getBaselineOffset(): Double = avatarSize * 0.75
    }
    private val initialIcon = InitialIcon(avatarSize)
    private val imageView = ImageView()

    init {
        val imageAvl = booleanBinding(imageView.imageProperty()) { value != null }
        initialIcon.root.removeWhen { imageAvl }

        root.minHeight = avatarSize
        root.minWidth = avatarSize
        root.add(initialIcon.root)
        root.add(imageView)
    }

    fun updateName(name: String, color: Color) {
        val c = extract_key_chars(name)
        this.initialIcon.updateItem(c.first, c.second, color)
    }

    fun updateUrl(url:HttpUrl?) {
        this.imageView.imageProperty().unbind()
        this.imageView.image = null
        if (url!=null) {
            val i = downloadImageResized(url, avatarSize, client = client)
            this.imageView.imageProperty().bind(i)
        }
    }
}
