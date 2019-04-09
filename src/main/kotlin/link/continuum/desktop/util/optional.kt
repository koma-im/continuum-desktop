package link.continuum.desktop.util

sealed class Option<out T> {
    fun isSome(): Boolean = when (this) {
        is Some -> true
        is None -> false
    }
    fun isNone() = !this.isSome()
    inline fun onSome(action: (T)->Unit) {
        if (this is Some) {
            action(this.value)
        }
    }
    inline fun<U> map(f: (T)->U): Option<U> = when(this) {
        is Some -> Some(f(this.value))
        is None -> None()
    }

    override fun toString(): String = when(this) {
        is Some -> "Some(${this.value})"
        is None -> "None"
    }
}

fun<T> T?.toOption(): Option<T> = this?.let { Some(it) }?:None()

class Some<out T>(val value: T): Option<T>()
class None<out T>(): Option<T>()
