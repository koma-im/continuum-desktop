package koma.gui.element.control.inputmap.mapping

import javafx.event.Event
import javafx.event.EventHandler
import javafx.event.EventType
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import koma.gui.element.control.inputmap.KeyBinding
import java.util.*
import java.util.function.Predicate


/**
 * The KeyMapping class provides API to specify
 * [mappings][InputMap.Mapping] related to key input.
 */
class KeyMapping
/**
 * Creates a new KeyMapping instance that will fire when the given
 * [KeyBinding] is entered into the application by the user, and this
 * will result in the given [EventHandler] being fired, as long as the
 * given interceptor is not true.
 *
 * @param keyBinding The [KeyBinding] to listen for.
 * @param eventHandler The [EventHandler] to fire when the
 * [KeyBinding] is observed.
 * @param interceptor A [Predicate] that, if true, will prevent the
 * [EventHandler] from being fired.
 */
constructor(private val keyBinding: KeyBinding,
            eventHandler: EventHandler<KeyEvent>,
            interceptor: Predicate<KeyEvent>? = null)
    : Mapping<KeyEvent>(if (keyBinding == null) null else keyBinding!!.type, eventHandler) {

    constructor(keyBinding: KeyBinding,
                eventHandler: (KeyEvent) -> Unit,
                interceptor: Predicate<KeyEvent>? = null)
            : this(keyBinding,
            EventHandler<KeyEvent> { event -> event ?.let { eventHandler(it) } },
            interceptor)

    /** {@inheritDoc}  */
    override val mappingKey: Any?
        get() = keyBinding

    /**
     * Creates a new KeyMapping instance that will fire when the given
     * [KeyCode] is entered into the application by the user, and this
     * will result in the given [EventHandler] being fired. The
     * eventType argument can be one of the following:
     *
     *
     *  * [KeyEvent.ANY]
     *  * [KeyEvent.KEY_PRESSED]
     *  * [KeyEvent.KEY_TYPED]
     *  * [KeyEvent.KEY_RELEASED]
     *
     *
     * @param keyCode The [KeyCode] to listen for.
     * @param eventType The type of [KeyEvent] to listen for.
     * @param eventHandler The [EventHandler] to fire when the
     * [KeyCode] is observed.
     */
    constructor(keyCode: KeyCode,
                eventType: EventType<KeyEvent>,
                eventHandler: EventHandler<KeyEvent>)
            : this(KeyBinding(keyCode, eventType), eventHandler) {}

    constructor(keyCode: KeyCode,
                eventHandler: (KeyEvent) -> Unit,
                interceptor: Predicate<KeyEvent>? = null)
            : this(KeyBinding(keyCode),
            EventHandler<KeyEvent> { event -> event ?.let { eventHandler(it) } },
            interceptor)

    init {
        interceptor?.let { setInterceptor(it) }
    }

    override fun getSpecificity(e: Event): Int {
        if (isDisabled) return 0
        return if (e !is KeyEvent) 0 else keyBinding.getSpecificity(e as KeyEvent)
    }

    /** {@inheritDoc}  */
    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o !is KeyMapping) return false
        if (!super.equals(o)) return false

        val that = o as KeyMapping?

        // we know keyBinding is non-null here
        return keyBinding.equals(that!!.keyBinding)
    }

    /** {@inheritDoc}  */
    override fun hashCode(): Int {
        return Objects.hash(keyBinding)
    }
}
