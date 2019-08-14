package koma.gui.element.control

import javafx.scene.control.IndexedCell
import javafx.scene.control.ListCell
import javafx.scene.control.ScrollBar
import javafx.scene.control.Skin
import koma.gui.element.control.skin.KVirtualFlow
import koma.gui.element.control.skin.ScrollBarSkin
import org.controlsfx.tools.Utils

/**
 * This custom ScrollBar is used to map the increment & decrement features
 * to pixel based scrolling rather than thumb/track based scrolling, if the
 * "virtual" attribute is true.
 */
class KVirtualScrollBar<I, T>(
        private val flow: KVirtualFlow<I, T>,
        private val isVirtual: Boolean = true
) : ScrollBar()
        where I: ListCell<T> {

    override fun decrement() {
        if (isVirtual) {
            flow.scrollPixels(-10.0)
        } else {
            super.decrement()
        }
    }

    override fun increment() {
        if (isVirtual) {
            flow.scrollPixels(10.0)
        } else {
            super.increment()
        }
    }

    fun turnPageByPos(pos: Double) {
        val oldValue = flow.getPosition()

        val newValue = (max - min) * Utils.clamp(0.0, pos, 1.0) + min
        if (newValue < oldValue) {
            pageUp()
        } else if (newValue > oldValue) {
            pageDown()
        }
    }

    fun pageUp(){
        val cell = flow.firstVisibleCell ?: return
        flow.scrollToBottom(cell)
    }

    fun pageDown(){
        val cell = flow.lastVisibleCell ?: return
        flow.scrollToTop(cell)
    }

    override fun createDefaultSkin(): Skin<*> {
        return ScrollBarSkin(this)
    }
}
