package koma.network.media

import javafx.beans.property.SimpleObjectProperty
import javafx.scene.image.Image
import koma.util.result.ok
import kotlinx.coroutines.experimental.javafx.JavaFx
import kotlinx.coroutines.experimental.launch
import okhttp3.HttpUrl
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
    private val cache: Cache<HttpUrl, ImageProperty>

    init {
        cache = createCache()
    }

    fun getProcImg(url: HttpUrl, cacheDays: Int? = null): ImageProperty
            = cache.computeIfAbsent(url, { createImageProperty(url, cacheDays) })

    private fun createImageProperty(url: HttpUrl, cacheDays: Int?): ImageProperty{
        val prop = ImageProperty()
        launch {
            val bs = downloadMedia(url, cacheDays).ok()
            bs ?: return@launch
            val img = processing(bs.inputStream())
            launch(JavaFx) { prop.set(img) }
        }
        return prop
    }

    private fun createCache(): Cache<HttpUrl, ImageProperty> {
        val conf = Cache2kConfiguration<HttpUrl, ImageProperty>()
        val cache = Cache2kBuilder.of(conf)
                .entryCapacity(300)
        return cache.build()
    }
}
