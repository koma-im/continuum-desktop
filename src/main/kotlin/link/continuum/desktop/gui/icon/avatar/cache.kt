package link.continuum.desktop.gui.icon.avatar

import javafx.beans.property.SimpleObjectProperty
import javafx.scene.image.Image
import koma.Server
import koma.network.media.MHUrl
import koma.util.getOr
import koma.util.testFailure
import kotlinx.coroutines.*
import link.continuum.desktop.gui.UiDispatcher
import link.continuum.desktop.util.None
import link.continuum.desktop.util.Option
import link.continuum.desktop.util.Some
import mu.KotlinLogging
import org.cache2k.Cache
import org.cache2k.Cache2kBuilder
import org.cache2k.configuration.Cache2kConfiguration
import java.io.InputStream

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
