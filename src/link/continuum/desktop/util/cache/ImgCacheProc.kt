package link.continuum.desktop.util.cache

import javafx.beans.property.SimpleObjectProperty
import javafx.scene.image.Image
import koma.koma_app.appState
import koma.network.media.MHUrl
import koma.network.media.downloadMedia
import koma.util.result.ok
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.javafx.JavaFx
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.cache2k.Cache
import org.cache2k.Cache2kBuilder
import org.cache2k.configuration.Cache2kConfiguration
import java.io.InputStream

typealias ImageProperty = SimpleObjectProperty<Image>
/**
 * returns image property immediately
 * download image, apply processing in the background
 *
 * currently used for avatars, which appears many times
 * using the same Image is probably more efficient
 */
open class ImgCacheProc(val processing: (InputStream) -> Image) {
    private val cache: Cache<MHUrl, ImageProperty>
    private val server = appState.serverConf

    init {
        cache = createCache()
    }

    fun getProcImg(url: MHUrl): ImageProperty
            = cache.computeIfAbsent(url, { createImageProperty(url) })

    private fun createImageProperty(url: MHUrl): ImageProperty {
        val prop = ImageProperty()
        GlobalScope.launch {
            val bs = appState.koma.downloadMedia(url, server).ok()
            bs ?: return@launch
            val img = processing(bs.inputStream())
            withContext(Dispatchers.JavaFx) { prop.set(img) }
        }
        return prop
    }

    private fun createCache(): Cache<MHUrl, ImageProperty> {
        val conf = Cache2kConfiguration<MHUrl, ImageProperty>()
        val cache = Cache2kBuilder.of(conf)
                .entryCapacity(300)
        return cache.build()
    }
}
