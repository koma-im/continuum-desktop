package koma.gui.element.control.inputmap.mapping

import javafx.event.Event
import javafx.scene.input.KeyEvent
import koma.gui.element.control.inputmap.KeyBinding

import java.util.function.Predicate

/**
 * Convenience class that can act as an keyboard input interceptor, either at a
 * [input map][InputMap.interceptorProperty] level or a
 * [mapping][Mapping.interceptorProperty] level.
 *
 * @see InputMap.interceptorProperty
 * @see Mapping.interceptorProperty
 */
class KeyMappingInterceptor
/**
 * Creates a new KeyMappingInterceptor, which will block execution of
 * event handlers (either at a
 * [input map][InputMap.interceptorProperty] level or a
 * [mapping][Mapping.interceptorProperty] level), where the input
 * received is equal to the given [KeyBinding].
 *
 * @param keyBinding The [KeyBinding] for which mapping execution
 * should be blocked.
 */
(private val keyBinding: KeyBinding) : Predicate<Event> {

    /**  {@inheritDoc}  */
    override fun test(event: Event): Boolean {
        return if (event !is KeyEvent) false else KeyBinding.toKeyBinding(event) == keyBinding
    }
}
