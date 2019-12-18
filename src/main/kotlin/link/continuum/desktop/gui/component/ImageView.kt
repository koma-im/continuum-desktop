package link.continuum.desktop.gui.component

import javafx.scene.Node
import javafx.scene.image.ImageView
import koma.Server
import koma.network.media.MHUrl
import koma.util.testFailure
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import link.continuum.desktop.gui.prop
import link.continuum.desktop.observable.MutableObservable
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

class MxcImageView {
    private val scope = MainScope()
    val root: Node = ImageView().apply {
        isPreserveRatio = true
        isSmooth = true
    }
    var fitWidth by prop((root as ImageView).fitWidthProperty())
    var fitHeight by prop((root as ImageView).fitHeightProperty())
    val image by prop((root as ImageView).imageProperty())
    private val urlFlow = MutableObservable<Pair<MHUrl, Server>>()
    fun setMxc(mxc: MHUrl, server: Server) {
        urlFlow.set(mxc to server)
    }
    init {
        urlFlow.flow()
                .distinctUntilChanged { old, new -> old.first != new.first }
                .onEach {
                    (root as ImageView).image = null
                }
                .mapLatest {
                    val w = 32.0.coerceAtLeast(fitWidth)
                    val h = 32.0.coerceAtLeast(fitHeight)
                    logger.trace { "Image view size $w $h" }
                    downloadImageSized(it.first, it.second, w, h)
                }.onEach {
                    val (img, failure, result) = it
                    if (!result.testFailure(img, failure)) {
                        (root as ImageView).image = img
                    }
                }.launchIn(scope)
    }
    fun cancelScope() {
        scope.cancel()
    }
}