package link.continuum.desktop.gui.component

import javafx.beans.property.SimpleObjectProperty
import javafx.scene.image.Image
import javafx.scene.layout.*
import koma.Server
import koma.network.media.MHUrl
import koma.util.testFailure
import kotlinx.coroutines.*
import mu.KotlinLogging
import java.util.concurrent.atomic.AtomicReference

private val logger = KotlinLogging.logger {}

class FitImageRegion(
        /**
         * scale up the image to cover the entire region
         */
        cover: Boolean = true
): Region(), CoroutineScope by CoroutineScope(Dispatchers.Default) {
    val imageProperty = SimpleObjectProperty<Image?>()
    var image
        get() = imageProperty.get()
        set(value) {imageProperty.set(value)}
    private val job = AtomicReference<Job?>()
    fun setMxc(mxc: MHUrl, server: Server) {
        val j = launch {
            val (bs, failure, result) = server.downloadMedia(mxc)
            if (result.testFailure(bs, failure)) {
                logger.debug { "downloading of $mxc failed" }
                return@launch
            }
            val region = this@FitImageRegion
            val img = bs.inputStream().use {
                Image(it,
                        32.0.coerceAtLeast(region.width),
                        32.0.coerceAtLeast(region.height),
                        true , true)
            }
            withContext(Dispatchers.Main) {
                image = img
            }
        }
        val prev = job.getAndSet(j)
        prev?.cancel()
    }
    init {
        imageProperty.addListener { _, _, image ->
            if (image == null) {
                backgroundProperty().set(null)
                return@addListener
            }
            val backgroundImage = if (cover) {
                BackgroundImage(image,
                        BackgroundRepeat.NO_REPEAT,
                        BackgroundRepeat.NO_REPEAT,
                        BackgroundPosition.CENTER,
                        bgSize
                )
            } else {
                BackgroundImage(image,
                        BackgroundRepeat.NO_REPEAT,
                        BackgroundRepeat.NO_REPEAT,
                        BackgroundPosition.CENTER,
                        bgSizeContain)
            }
            this.backgroundProperty().set(Background(backgroundImage))
        }
    }
    companion object {
        private val bgSize =  BackgroundSize(100.0, 100.0,
                true, true,
                false, true)
        private val bgSizeContain =  BackgroundSize(100.0, 100.0,
                true, true,
                true, false)
    }
}