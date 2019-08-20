package koma.gui.element.control.inputmap

import javafx.event.EventType
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import koma.gui.element.control.Utils
import java.util.*

/**
 * KeyBindings are used to describe which action should occur based on some
 * KeyEvent state and Control state. These bindings are used to populate the
 * keyBindings variable on BehaviorBase. The KeyBinding can be subclassed to
 * add additional matching criteria. A match in a subclass should always have
 * a specificity that is 1 greater than its superclass in the case of a match,
 * or 0 in the case where there is no match.
 *
 * Note that this API is, at present, quite odd in that you use a constructor
 * and then use shift(), ctrl(), alt(), or meta() separately. It gave me an
 * object-literal like approach but isn't ideal. We will want some builder
 * approach here (similar as in other places).
 *
 * @since 9
 */
class KeyBinding @JvmOverloads constructor(val code: KeyCode?, type: EventType<KeyEvent>? = null) {
    val type: EventType<KeyEvent> = type ?: KeyEvent.KEY_PRESSED
    var shift = OptionalBoolean.FALSE
        private set
    var ctrl = OptionalBoolean.FALSE
        private set
    var alt = OptionalBoolean.FALSE
        private set
    var meta = OptionalBoolean.FALSE
        private set

    /**
     * Designed for 'catch-all' situations, e.g. all KeyTyped events.
     * @param type
     */
    constructor(type: EventType<KeyEvent>) : this(null, type) {}

    @JvmOverloads
    fun shift(value: OptionalBoolean = OptionalBoolean.TRUE): KeyBinding {
        shift = value
        return this
    }

    @JvmOverloads
    fun ctrl(value: OptionalBoolean = OptionalBoolean.TRUE): KeyBinding {
        ctrl = value
        return this
    }

    @JvmOverloads
    fun alt(value: OptionalBoolean = OptionalBoolean.TRUE): KeyBinding {
        alt = value
        return this
    }

    @JvmOverloads
    fun meta(value: OptionalBoolean = OptionalBoolean.TRUE): KeyBinding {
        meta = value
        return this
    }

    /**
     * usually ctrl
     * except mac
     */
    fun shortcut(): KeyBinding {
        when (Utils.getPlatformShortcutKey()) {
            KeyCode.SHIFT -> return shift()

            KeyCode.CONTROL -> return ctrl()

            KeyCode.ALT -> return alt()

            KeyCode.META -> return meta()

            else -> return this
        }
    }

    fun getSpecificity(event: KeyEvent): Int {
        var s: Int
        if (code != null && code != event.code) return 0 else s = 1
        if (!shift.equals(event.isShiftDown)) return 0 else if (shift != OptionalBoolean.ANY) s++
        if (!ctrl.equals(event.isControlDown)) return 0 else if (ctrl != OptionalBoolean.ANY) s++
        if (!alt.equals(event.isAltDown)) return 0 else if (alt != OptionalBoolean.ANY) s++
        if (!meta.equals(event.isMetaDown)) return 0 else if (meta != OptionalBoolean.ANY) s++
        if (type != event.eventType) return 0 else s++
        // We can now trivially accept it
        return s
    }

    /** {@inheritDoc}  */
    override fun toString(): String {
        return "KeyBinding [code=" + code + ", shift=" + shift +
                ", ctrl=" + ctrl + ", alt=" + alt +
                ", meta=" + meta + ", type=" + type + "]"
    }

    /** {@inheritDoc}  */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is KeyBinding) return false
        val that = other as KeyBinding?
        return code == that!!.code &&
                type == that.type &&
                shift == that.shift &&
                ctrl == that.ctrl &&
                alt == that.alt &&
                meta == that.meta
    }

    /** {@inheritDoc}  */
    override fun hashCode(): Int {
        return Objects.hash(code, type, shift, ctrl, alt, meta)
    }

    /**
     * A tri-state boolean used with KeyBinding.
     */
    enum class OptionalBoolean {
        TRUE,
        FALSE,
        ANY;

        fun equals(b: Boolean): Boolean {
            if (this == ANY) return true
            if (b && this == TRUE) return true
            return if (!b && this == FALSE) true else false
        }
    }

    companion object {

        fun toKeyBinding(keyEvent: KeyEvent): KeyBinding {
            val newKeyBinding = KeyBinding(keyEvent.code, keyEvent.eventType)
            if (keyEvent.isShiftDown) newKeyBinding.shift()
            if (keyEvent.isControlDown) newKeyBinding.ctrl()
            if (keyEvent.isAltDown) newKeyBinding.alt()
            if (keyEvent.isShortcutDown) newKeyBinding.shortcut()
            return newKeyBinding
        }
    }

}
