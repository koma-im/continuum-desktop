package link.continuum.desktop.gui.icon.avatar

import javafx.scene.image.ImageView
import javafx.scene.layout.StackPane
import javafx.scene.paint.Color
import koma.Server
import koma.network.media.MHUrl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import link.continuum.desktop.gui.add
import link.continuum.desktop.gui.booleanBinding
import link.continuum.desktop.gui.removeWhen
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

@ExperimentalCoroutinesApi
class UrlAvatar(
        private val avatarSize: Double
): CoroutineScope by CoroutineScope(Dispatchers.Default) {
    val root = object :StackPane() {
        // roughly aligned with text vertically
        override fun getBaselineOffset(): Double = avatarSize * 0.75
    }
    private val initialIcon = InitialIcon(avatarSize)
    private val imageView = ImageView()

    init {
        imageView.imageProperty()
        val imageAvl = booleanBinding(imageView.imageProperty()) { value != null }
        initialIcon.root.removeWhen(imageAvl)

        root.minHeight = avatarSize
        root.minWidth = avatarSize
        root.add(initialIcon.root)
        root.add(imageView)
    }

    fun updateName(name: String, color: Color) {
        this.initialIcon.updateItem(name, color)
    }

    fun updateUrl(url: MHUrl?, server: Server) {
        this.imageView.imageProperty().unbind()
        this.imageView.image = null
        if (url!=null) {
            val i = downloadImageResized(url, avatarSize, server)
            this.imageView.imageProperty().bind(i)
        }
    }
}
