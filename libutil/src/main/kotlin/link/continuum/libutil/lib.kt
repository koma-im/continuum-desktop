package link.continuum.libutil

import java.util.*

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