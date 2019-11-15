package link.continuum.desktop.gui.icon.avatar

import javafx.application.Platform
import javafx.scene.image.ImageView
import javafx.scene.paint.Color
import javafx.scene.shape.Circle
import javafx.scene.shape.Rectangle
import koma.Server
import koma.network.media.MHUrl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import link.continuum.desktop.gui.*
import link.continuum.desktop.gui.component.FitImageRegion
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

class UrlAvatar(
): CoroutineScope by CoroutineScope(Dispatchers.Default) {
    val root = object :StackPane() {
        // roughly aligned with text vertically
        override fun getBaselineOffset(): Double = height * 0.75
    }
    private val initialIcon = InitialIcon()
    private val imageView = FitImageRegion()

    init {
        initialIcon.root.removeWhen(imageView.imageProperty.isNotNull)

        root.add(initialIcon.root)
        root.add(imageView)
    }

    fun updateName(name: String, color: Color) {
        check(Platform.isFxApplicationThread())
        this.initialIcon.updateItem(name, color)
    }

    fun updateUrl(url: MHUrl?, server: Server) {
        url?:return
        imageView.setMxc(url, server)
    }

    companion object {
    }
}
