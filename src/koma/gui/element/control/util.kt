package koma.gui.element.control

import javafx.application.ConditionalFeature
import javafx.application.Platform
import javafx.scene.input.KeyCode


public object Utils {
    private val os = System.getProperty("os.name")
    val MAC = os.startsWith("Mac")
    public val isTwoLevelFocus = Platform.isSupported(ConditionalFeature.TWO_LEVEL_FOCUS)
    val IS_TOUCH_SUPPORTED = Platform.isSupported(ConditionalFeature.INPUT_TOUCH)

    fun getPlatformShortcutKey(): KeyCode {
        return if (MAC) KeyCode.META else KeyCode.CONTROL
    }

    fun boundedSize(value: Double, min: Double, max: Double): Double {
        // if max < value, return max
        // if min > value, return min
        // if min > max, return min
        return Math.min(Math.max(value, min), Math.max(min, max))
    }

    fun clamp(min: Double, value: Double, max: Double): Double {
        if (value < min) return min
        return if (value > max) max else value
    }

}
