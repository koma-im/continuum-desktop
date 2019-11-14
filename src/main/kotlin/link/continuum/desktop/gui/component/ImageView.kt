package link.continuum.desktop.gui.component

import javafx.scene.Node
import javafx.scene.image.ImageView
import koma.Server
import koma.network.media.MHUrl
import koma.util.testFailure
import kotlinx.coroutines.*
import link.continuum.desktop.gui.prop
import mu.KotlinLogging
import java.util.concurrent.atomic.AtomicReference

private val logger = KotlinLogging.logger {}

class MxcImageView(
): CoroutineScope by CoroutineScope(Dispatchers.Default) {
    val root: Node = ImageView().apply {
        isPreserveRatio = true
        isSmooth = true
    }
    var fitWidth by prop((root as ImageView).fitWidthProperty())
    var fitHeight by prop((root as ImageView).fitHeightProperty())
    val image by prop((root as ImageView).imageProperty())
    private val job = AtomicReference<Job?>()
    fun setMxc(mxc: MHUrl, server: Server) {
        val j = launch {
            root as ImageView
            withContext(Dispatchers.Main) {
                root.image = null
                val w = 32.0.coerceAtLeast(fitWidth)
                val h = 32.0.coerceAtLeast(fitHeight)
                logger.trace { "Image view size $w $h" }
                val (img, failure, result) = downloadImageSized(mxc, server, w, h)
                if (result.testFailure(img, failure)) {
                    return@withContext
                } else {
                    root.image = img
                }
            }
        }
        val prev = job.getAndSet(j)
        prev?.cancel()
    }
}