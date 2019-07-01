package link.continuum.desktop.util.cache

import javafx.beans.property.SimpleObjectProperty
import javafx.scene.image.Image
import koma.util.getOr
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.javafx.JavaFx
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import link.continuum.desktop.util.http.downloadHttp
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
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
open class ImgCacheProc(
        val processing: (InputStream) -> Image,
        private val client: OkHttpClient,
        private val maxStale: Int = 1000
) {
    private val cache: Cache<HttpUrl, ImageProperty>

    init {
        cache = createCache()
    }

    fun getImg(url: HttpUrl): ImageProperty {
        return cache.computeIfAbsent(url) { createImageProperty(url) }
    }

    private fun createImageProperty(url: HttpUrl): ImageProperty {
        val prop = ImageProperty()
        GlobalScope.launch {
            val bs = downloadHttp(url, client, maxStale) getOr { return@launch }
            val img = processing(bs.inputStream())
            withContext(Dispatchers.JavaFx) { prop.set(img) }
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
