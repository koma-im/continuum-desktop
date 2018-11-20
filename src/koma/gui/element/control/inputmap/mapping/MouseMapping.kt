package koma.gui.element.control.inputmap.mapping

import javafx.event.Event
import javafx.event.EventHandler
import javafx.event.EventType
import javafx.scene.input.KeyCode
import javafx.scene.input.MouseEvent

/**
 * The MouseMapping class provides API to specify
 * Mapping mappings related to mouse input.
 */
class MouseMapping
/**
 * Creates a new KeyMapping instance that will fire when the given
 * [KeyCode] is entered into the application by the user, and this
 * will result in the given [EventHandler] being fired. The
 * eventType argument can be any of the [MouseEvent] event types,
 * but typically it is one of the following:
 *
 *
 *  * [MouseEvent.ANY]
 *  * [MouseEvent.MOUSE_PRESSED]
 *  * [MouseEvent.MOUSE_CLICKED]
 *  * [MouseEvent.MOUSE_RELEASED]
 *
 *
 * @param eventType The type of [MouseEvent] to listen for.
 * @param eventHandler The [EventHandler] to fire when the
 * [MouseEvent] is observed.
 */
(eventType: EventType<MouseEvent>?, eventHandler: EventHandler<MouseEvent>) : Mapping<MouseEvent>(eventType, eventHandler) {
    init {
        if (eventType == null) {
            throw IllegalArgumentException("MouseMapping eventType constructor argument can not be null")
        }
    }

    /** {@inheritDoc}  */
    override fun getSpecificity(e: Event): Int {
        if (isDisabled) return 0
        if (e !is MouseEvent) return 0
        val et = eventType

        // FIXME naive
        var s = 0
        if (e.eventType == MouseEvent.MOUSE_CLICKED && et != MouseEvent.MOUSE_CLICKED) return 0 else s++
        if (e.eventType == MouseEvent.MOUSE_DRAGGED && et != MouseEvent.MOUSE_DRAGGED) return 0 else s++
        if (e.eventType == MouseEvent.MOUSE_ENTERED && et != MouseEvent.MOUSE_ENTERED) return 0 else s++
        if (e.eventType == MouseEvent.MOUSE_ENTERED_TARGET && et != MouseEvent.MOUSE_ENTERED_TARGET) return 0 else s++
        if (e.eventType == MouseEvent.MOUSE_EXITED && et != MouseEvent.MOUSE_EXITED) return 0 else s++
        if (e.eventType == MouseEvent.MOUSE_EXITED_TARGET && et != MouseEvent.MOUSE_EXITED_TARGET) return 0 else s++
        if (e.eventType == MouseEvent.MOUSE_MOVED && et != MouseEvent.MOUSE_MOVED) return 0 else s++
        if (e.eventType == MouseEvent.MOUSE_PRESSED && et != MouseEvent.MOUSE_PRESSED) return 0 else s++
        if (e.eventType == MouseEvent.MOUSE_RELEASED && et != MouseEvent.MOUSE_RELEASED) return 0 else s++

        // TODO handle further checks

        return s
    }
}
