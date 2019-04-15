package link.continuum.desktop.gui.icon.avatar

import javafx.beans.property.SimpleObjectProperty
import javafx.scene.image.Image
import kotlinx.coroutines.*
import link.continuum.desktop.gui.UiDispatcher
import link.continuum.desktop.util.None
import link.continuum.desktop.util.Option
import link.continuum.desktop.util.Some
import link.continuum.desktop.util.getOr
import link.continuum.desktop.util.http.downloadHttp
import link.continuum.libutil.`?or`
import mu.KotlinLogging
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import org.cache2k.Cache
import org.cache2k.Cache2kBuilder
import org.cache2k.configuration.Cache2kConfiguration
import java.io.InputStream

private val logger = KotlinLogging.logger {}
private typealias Item = Deferred<Option<Image>>

class DeferredImage(
        val processing: (InputStream) -> Image,
        private val maxStale: Int = 1000
) {
    private val cache: Cache<HttpUrl, Item>

    init {
        cache = createCache()
    }

    fun getDeferred(url: HttpUrl, client: OkHttpClient): Item {
        return cache.computeIfAbsent(url) { createImageProperty(url, client) }
    }

    fun getDeferred(url: String, client: OkHttpClient): Item {
        val h = HttpUrl.parse(url) `?or` {
            logger.warn { "invalid image url" }
            return CompletableDeferred(None())
        }
        return getDeferred(h, client)
    }

    private fun createImageProperty(url: HttpUrl, client: OkHttpClient): Item {
        return GlobalScope.asyncImage(url, client)
    }

    private fun CoroutineScope.asyncImage(url: HttpUrl, client: OkHttpClient) = async {
        val bs = downloadHttp(url, client, maxStale) getOr {
            logger.warn { "image $url not downloaded, ${it.error}, returning None" }
            return@async None<Image>()
        }
        val img = processing(bs.inputStream())
        Some(img)
    }

    private fun createCache(): Cache<HttpUrl, Item> {
        val conf = Cache2kConfiguration<HttpUrl, Item>()
        val cache = Cache2kBuilder.of(conf)
                .entryCapacity(100)
        return cache.build()
    }
}


private typealias ImageProperty = SimpleObjectProperty<Image>

fun downloadImageResized(url: HttpUrl, size: Double, client: OkHttpClient): ImageProperty {
    val prop = ImageProperty()
    GlobalScope.launch {
        val bs = downloadHttp(url, client) getOr  {
            logger.error { "download of $url fails with ${it.error}" }
            return@launch
        }
        val img = bs.inputStream().use {
            Image(it, size, size, true , true)
        }
        withContext(UiDispatcher) { prop.set(img) }
    }
    return prop
}
