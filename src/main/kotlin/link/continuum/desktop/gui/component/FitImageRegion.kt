package link.continuum.desktop.gui.component

import javafx.scene.image.Image
import javafx.scene.layout.*
import koma.KomaFailure
import koma.Server
import koma.network.media.MHUrl
import koma.util.KResult
import koma.util.testFailure
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import link.continuum.desktop.observable.set
import mu.KotlinLogging

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
    logger.trace { "downloaded original $mxc"}
    return KResult.success(img)
}

class FitImageRegion(
        /**
         * scale up the image to cover the entire region
         */
        cover: Boolean = true
): Region() {
    private val scope = MainScope()
    private lateinit var  server: Server
    val urlFlow = MutableStateFlow<MHUrl?>(null)
    val imageProperty = MutableStateFlow<Image?>(null)
    var image
        get() = imageProperty.value
        set(value) {imageProperty.value =  value}

    /**
     * null url clears the image
     */
    fun setMxc(
            mxc: MHUrl?,
            server: Server
    ) {
        this.server = server
        urlFlow.set(mxc)
    }
    init {
        scope.launch {
            urlFlow.onEach {
                if (it == null) {
                    imageProperty.set(null)
                }
            }
                    .filterNotNull()
                    .distinctUntilChanged()
                    .collectLatest { u ->
                        val r = this@FitImageRegion
                        val w = r.width.coerceAtLeast(32.0)
                        val h = r.height.coerceAtLeast(32.0)
                        logger.trace { "Image view size $w $h" }
                        val (img, _, _) = downloadImageSized(u, server, w, h)
                        img ?: return@collectLatest
                        imageProperty.set(img)
                    }
        }
        imageProperty.onEach {
            if (image == null) {
                backgroundProperty().set(null)
                return@onEach
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
        }.launchIn(scope)
    }
    fun close() {
        scope.cancel()
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