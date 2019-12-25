package link.continuum.desktop.gui.icon.avatar

import javafx.beans.property.SimpleObjectProperty
import javafx.scene.image.Image
import koma.Server
import koma.network.media.MHUrl
import koma.util.testFailure
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import link.continuum.desktop.gui.UiDispatcher
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

private typealias ImageProperty = SimpleObjectProperty<Image>

fun CoroutineScope.downloadImageResized(url: MHUrl, size: Double, server: Server): ImageProperty {
    val prop = ImageProperty()
    launch {
        val (bs, it, result) = server.downloadMedia(url)
        if (result.testFailure(bs, it)) {
            logger.error { "download of $url fails with ${it}" }
            return@launch
        }
        val img = bs.inputStream().use {
            Image(it, size, size, true , true)
        }
        withContext(UiDispatcher) { prop.set(img) }
    }
    return prop
}
