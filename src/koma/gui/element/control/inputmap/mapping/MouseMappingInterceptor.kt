package koma.gui.element.control.inputmap.mapping

import javafx.event.Event
import javafx.event.EventType
import javafx.scene.input.MouseEvent

import java.util.function.Predicate

/**
 * Convenience class that can act as a mouse input interceptor, either at a
 * [input map][InputMap.interceptorProperty] level or a
 * [mapping][Mapping.interceptorProperty] level.
 *
 * @see InputMap.interceptorProperty
 * @see Mapping.interceptorProperty
 */
class MouseMappingInterceptor
/**
 * Creates a new MouseMappingInterceptor, which will block execution of
 * event handlers (either at a
 * [input map][InputMap.interceptorProperty] level or a
 * [mapping][Mapping.interceptorProperty] level), where the input
 * received is equal to the given [EventType].
 *
 * @param eventType The [EventType] for which mapping execution
 * should be blocked (typically one of
 * [MouseEvent.MOUSE_PRESSED],
 * [MouseEvent.MOUSE_CLICKED], or
 * [MouseEvent.MOUSE_RELEASED]).
 */
(private val eventType: EventType<MouseEvent>) : Predicate<Event> {

    /**  {@inheritDoc}  */
    override fun test(event: Event): Boolean {
        return if (event !is MouseEvent) false else event.eventType == this.eventType
    }
}
