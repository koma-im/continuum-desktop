package link.continuum.desktop.gui.icon.avatar

import javafx.application.Platform
import javafx.geometry.Insets
import javafx.scene.effect.DropShadow
import javafx.scene.image.ImageView
import javafx.scene.layout.*
import javafx.scene.paint.Color
import javafx.scene.shape.Circle
import javafx.scene.shape.Rectangle
import koma.Server
import koma.network.media.MHUrl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import link.continuum.desktop.gui.*
import link.continuum.desktop.gui.StackPane
import link.continuum.desktop.gui.component.FitImageRegion
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

abstract class UrlAvatar(
): CoroutineScope by CoroutineScope(Dispatchers.Default) {
    val root: Region = object :StackPane() {
        // roughly aligned with text vertically
        override fun getBaselineOffset(): Double = height * 0.75
    }
    private val initialIcon = InitialIcon()
    private val imageView = FitImageRegion()

    init {
        initialIcon.root.removeWhen(imageView.imageProperty.isNotNull)

        root as StackPane
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

/**
 * width and height are about two lines
 */
class Avatar2L: UrlAvatar() {
    init {
        root as StackPane
        root.style = rootStyle
    }
    companion object {
        private val rootStyle = StyleBuilder {
            fixHeight(2.2.em)
            fixWidth(2.2.em)
        }.toStyle()
        private val clipStyle = StyleBuilder {
        }
    }
}


/**
 * width and height are about two lines
 */
class AvatarInline: UrlAvatar() {
    init {
        root as StackPane
        root.style = rootStyle
    }
    companion object {
        private val rootStyle = StyleBuilder {
            fixHeight(1.em)
            fixWidth(1.em)
        }.toStyle()
    }
}