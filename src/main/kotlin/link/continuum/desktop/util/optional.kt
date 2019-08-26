package link.continuum.desktop.util

import java.util.*

typealias Option<T> = Optional<T>

inline fun <T> Optional<T>.onSome(action: (T)->Unit): Optional<T> {
    if (this.isPresent) {
        action(this.get())
    }
    return this
}

inline fun <T> Optional<T>.onNone(action: ()->Unit): Optional<T> {
    if (this.isEmpty) {
        action()
    }
    return this
}

@Suppress("DEPRECATION", "FunctionName")
fun <T>None(): Optional<T> = Optional.empty()
@Suppress("DEPRECATION", "FunctionName")
fun <T>Some(value: T): Optional<T> = Optional.of(value)

fun<T: Any> T?.toOption(): Optional<T> = Optional.ofNullable(this)

inline infix fun<T: Any?> T.`?or?`(action: () -> T?): T? {
    if (this != null) return this
    return action()
}

inline infix fun<T: Any> T?.`?or`(action: () -> T): T {
    if (this != null) return this
    return action()
}

inline infix fun<T: Any> T?.onNull(action: ()->Unit): T? {
    if (this == null) action()
    return this
}

/**
 * get value of Optional if it's not empty
 * null otherwise
 */
fun <T> Optional<T>.getOrNull(): T? {
    if (this.isPresent) return this.get()
    return null
}