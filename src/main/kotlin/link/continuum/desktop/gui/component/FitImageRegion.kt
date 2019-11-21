package link.continuum.desktop.gui.component

import javafx.beans.property.SimpleObjectProperty
import javafx.scene.image.Image
import javafx.scene.layout.*
import koma.KomaFailure
import koma.Server
import koma.network.media.MHUrl
import koma.util.KResult
import koma.util.testFailure
import kotlinx.coroutines.*
import mu.KotlinLogging
import java.util.concurrent.atomic.AtomicReference

private val logger = KotlinLogging.logger {}

suspend fun downloadImageSized(
        mxc: MHUrl,
        server: Server,
        width: Double,
        height: Double
): KResult<Image, KomaFailure> {
    logger.trace { "downloading thumbnail sized ${width}x$height" }
    if (mxc is MHUrl.Mxc) {
        val (success, failure, result) = server.getThumbnail(mxc, width.toInt().toUShort(), height.toInt().toUShort())
        if (!result.testFailure(success, failure)) {
            val img = success.inputStream().use {
                Image(it)
            }
            // return must be outside the inline block of `use`
            return KResult.success(img)
        } else {
            logger.warn { "Couldn't download thumbnail sized $width by $height of $mxc"}
        }
    }
    val (bs, failure, result) = server.downloadMedia(mxc)
    if (result.testFailure(bs, failure)) {
        logger.debug { "downloading of $mxc failed" }
        return KResult.failure(failure)
    }
    val img = bs.inputStream().use {
        Image(it)
    }
    logger.debug { "downloaded original $mxc"}
    return KResult.success(img)
}

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
    fun setMxc(
            mxc: MHUrl,
            server: Server,
            width: Double = this.width.coerceAtLeast(32.0),
            height: Double = this.height.coerceAtLeast(32.0)
    ) {
        val j = launch {
            withContext(Dispatchers.Main) {
                image = null
                val (img, failure, result) = downloadImageSized(mxc, server, width, height)
                if (result.testFailure(img, failure)) {
                    return@withContext
                } else {
                    image = img
                }
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