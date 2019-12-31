@file:Suppress("DEPRECATION")

package link.continuum.desktop.util

import javafx.application.Platform
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

@Deprecated("")
@Suppress("ClassName")
@PublishedApi
internal object _Debug {
    @PublishedApi
    internal val enabled = logger.isDebugEnabled
    init {
        if (enabled) {
            logger.warn { "debug assertions enabled" }
        }
    }
}

inline fun whenDebugging(block: () -> Unit) {
    if (_Debug.enabled) {
        block()
    }
}

@Suppress("NOTHING_TO_INLINE")
inline fun debugAssertUiThread() {
    debugAssert({ Platform.isFxApplicationThread()}) { "Not on FxApplicationThread" }
}

@Suppress("NOTHING_TO_INLINE", "UNUSED")
inline fun debugAssert(value: Boolean) {
    debugAssert(value) { "Assertion failed" }
}

@Suppress("UNUSED")
inline fun debugAssert(block: () -> Boolean) {
    debugAssert(block) { "Assertion failed" }
}

inline fun debugAssert(value: Boolean, lazyMessage: () -> String) {
    whenDebugging {
        if (!value) {
            val message = lazyMessage()
            throw AssertionError(message)
        }
    }
}

inline fun debugAssert(block: () -> Boolean, lazyMessage: () -> Any) {
    whenDebugging {
        if (!block()) {
            val message = lazyMessage()
            throw AssertionError(message)
        }
    }
}