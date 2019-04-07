package link.continuum.desktop.util

sealed class Option<out T> {
    fun isSome(): Boolean = when (this) {
        is Some -> true
        is None -> false
    }
    inline fun onSome(action: (T)->Unit) {
        if (this is Some) {
            action(this.value)
        }
    }
}

class Some<out T>(val value: T): Option<T>()
class None<out T>(): Option<T>()
