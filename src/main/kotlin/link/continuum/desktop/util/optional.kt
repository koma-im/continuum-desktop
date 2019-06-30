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
