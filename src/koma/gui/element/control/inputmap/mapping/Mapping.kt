package koma.gui.element.control.inputmap.mapping

import javafx.beans.property.BooleanProperty
import javafx.beans.property.ObjectProperty
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.event.Event
import javafx.event.EventHandler
import javafx.event.EventType
import java.util.function.Predicate


/**
 * Abstract base class for all input mappings as used by the
 * [InputMap] class.
 *
 * @param <T> The type of [Event] the mapping represents.
*/
abstract class Mapping<T : Event>(
        /**
         * The [EventType] that is being listened for.
         */
        val eventType: EventType<T>?,
        /**
         * The [EventHandler] that will be fired should this mapping be
         * the most-specific mapping for a given input, and should it not be
         * blocked by an interceptor (either at a
         * [input map][InputMap.interceptorProperty] level or a
         * [mapping][Mapping.interceptorProperty] level).
         */
        val eventHandler: EventHandler<T>) {

    fun get_event_type(): EventType<T>? = eventType

    /***********************************************************************
     * *
     * Properties                                                          *
     * *
     */

    // --- disabled
    /**
     * By default all mappings are enabled (so this disabled property is set
     * to false by default). In some cases it is useful to be able to disable
     * a mapping until it is applicable. In these cases, users may simply
     * toggle the disabled property until desired.
     *
     *
     * When the disabled property is true, the mapping will not be
     * considered when input events are received, even if it is the most
     * specific mapping available.
     */
    private val disabled = SimpleBooleanProperty(this, "disabled", false)
    var isDisabled: Boolean
        get() = disabled.get()
        set(value) = disabled.set(value)


    // --- auto consume
    /**
     * By default mappings are set to 'auto consume' their specified event
     * handler. This means that the event handler will not propagate further,
     * but in some cases this is not desirable - sometimes it is preferred
     * that the event continue to 'bubble up' to parent nodes so that they
     * may also benefit from receiving this event. In these cases, it is
     * important that this autoConsume property be changed from the default
     * boolean true to instead be boolean false.
     */
    private val autoConsume = SimpleBooleanProperty(this, "autoConsume", true)
    var isAutoConsume: Boolean
        get() = autoConsume.get()
        set(value) = autoConsume.set(value)


    // --- interceptor
    /**
     * The role of the interceptor is to block the mapping on which it is
     * set from executing, whenever the interceptor returns true. The
     * interceptor is called every time the mapping is the best match for
     * a given input event, and is allowed to reason on the given input event
     * before returning a boolean value, where boolean true means block
     * execution, and boolean false means to allow execution.
     */
    private val interceptor = SimpleObjectProperty<Predicate<T>>(this, "interceptor")

    /**
     *
     * @return
     */
    open val mappingKey: Any?
        get() = eventType


    /***********************************************************************
     * *
     * Abstract methods                                                    *
     * *
     */

    /**
     * This method must be implemented by all mapping implementations such
     * that it returns an integer value representing how closely the mapping
     * matches the given [Event]. The higher the number, the greater
     * the match. This allows the InputMap to determine
     * which mapping is most specific, and to therefore fire the appropriate
     * mapping [EventHandler][Mapping.getEventHandler].
     *
     * @param event The [Event] that needs to be assessed for its
     * specificity.
     * @return An integer indicating how close of a match the mapping is to
     * the given Event. The higher the number, the greater the match.
     */
    abstract fun getSpecificity(event: Event): Int

    fun disabledProperty(): BooleanProperty {
        return disabled
    }

    fun autoConsumeProperty(): BooleanProperty {
        return autoConsume
    }

    fun getInterceptor(): Predicate<out Event>? {
        return interceptor.get()
    }

    fun testInterceptor(event: T): Boolean? {
        val interceptor = interceptor.get()
        val result = interceptor?.test(event)
        return result
    }


    fun setInterceptor(value: Predicate<T>) {
        interceptor.set(value)
    }

    fun interceptorProperty(): ObjectProperty<Predicate<T>> {
        return interceptor
    }

    /** {@inheritDoc}  */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Mapping<*>) return false

        val that = other as Mapping<*>?

        return !if (eventType != null) !eventType.equals(that!!.eventType) else that!!.eventType != null

    }

    /** {@inheritDoc}  */
    override fun hashCode(): Int {
        return if (eventType != null) eventType.hashCode() else 0
    }
}
