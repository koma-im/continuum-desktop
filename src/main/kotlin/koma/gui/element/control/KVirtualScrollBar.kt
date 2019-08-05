package koma.gui.element.control

import javafx.beans.property.BooleanProperty
import javafx.beans.property.SimpleBooleanProperty
import javafx.scene.control.IndexedCell
import javafx.scene.control.ScrollBar
import koma.gui.element.control.skin.KVirtualFlow
import org.controlsfx.tools.Utils

/**
 * This custom ScrollBar is used to map the increment & decrement features
 * to pixel based scrolling rather than thumb/track based scrolling, if the
 * "virtual" attribute is true.
 */
class KVirtualScrollBar<I, T>(private val flow: KVirtualFlow<I, T>
) : ScrollBar()
        where I: IndexedCell<T>{

    private var adjusting: Boolean = false
    private val virtual = SimpleBooleanProperty(this, "virtual")

    var isVirtual: Boolean
        get() = virtual.get()
        set(value) = virtual.set(value)

    init {

        super.valueProperty().addListener { _ ->
            if (isVirtual/* && oldValue != newValue*/) {
                if (adjusting) {
                    // no-op
                } else {
                    flow.setPosition(value)
                }
            }
        }
    }

    fun virtualProperty(): BooleanProperty {
        return virtual
    }


    /**************************************************************************
     *
     * Public API
     *
     */

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

    // this method is called when the user clicks in the scrollbar track, so
    // we special-case it to allow for page-up and page-down clicking to work
    // as expected.
    override fun adjustValue(pos: Double) {
        if (isVirtual) {
            adjusting = true
            val oldValue = flow.getPosition()

            val newValue = (max - min) * Utils.clamp(0.0, pos, 1.0) + min
            if (newValue < oldValue) {
                val cell = flow.firstVisibleCell ?: return
                flow.scrollToBottom(cell)
            } else if (newValue > oldValue) {
                val cell = flow.lastVisibleCell ?: return
                flow.scrollToTop(cell)
            }

            adjusting = false
        } else {
            super.adjustValue(pos)
        }
    }
}
