package koma.gui.element.control

import com.sun.javafx.scene.control.behavior.ScrollBarBehavior
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

class KScrollBarBehavior(private val bar: KVirtualScrollBar<*, *>): ScrollBarBehavior(bar) {
    fun end() {
        bar.end()
    }
    override fun trackPress(pos: Double) {
        val cur = (bar.getValue() - bar.getMin())/(bar.getMax() - bar.getMin())
        if (pos > cur) {
            logger.trace { "pg down" }
            bar.pageDown()
        } else if (pos < cur) {
            logger.trace { "pg up" }
            bar.pageUp()
        }
    }
}