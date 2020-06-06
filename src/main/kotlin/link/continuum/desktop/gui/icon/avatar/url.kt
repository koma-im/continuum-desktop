package link.continuum.desktop.gui.icon.avatar

import javafx.scene.layout.Region
import javafx.scene.paint.Color
import koma.Server
import koma.network.media.MHUrl
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import link.continuum.desktop.gui.StackPane
import link.continuum.desktop.gui.StyleBuilder
import link.continuum.desktop.gui.add
import link.continuum.desktop.gui.component.FitImageRegion
import link.continuum.desktop.gui.em
import link.continuum.desktop.util.debugAssertUiThread
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

abstract class UrlAvatar(
) {
    private val scope = MainScope()
    val root: Region = object :StackPane() {
        // roughly aligned with text vertically
        override fun getBaselineOffset(): Double = height * 0.75
    }
    val initialIcon = InitialIcon()
    val imageView = FitImageRegion()

    init {
        imageView.imageProperty.onEach {
            val noImage = it == null
            initialIcon.root.apply {
                isVisible = noImage
                isManaged = noImage
            }
        }.launchIn(scope)

        root as StackPane
        root.add(initialIcon.root)
        root.add(imageView)
    }

    @Deprecated("")
    fun updateName(name: String, color: Color) {
        debugAssertUiThread()
        this.initialIcon.updateItem(name, color)
    }

    /**
     * null url clears the image
     */
    fun updateUrl(url: MHUrl?, server: Server) {
        imageView.setMxc(url, server)
    }

    fun cancelScope() {
        scope.cancel()
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