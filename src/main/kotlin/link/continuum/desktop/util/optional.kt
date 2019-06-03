package link.continuum.desktop.util

import java.util.*

@Suppress("NON_PUBLIC_PRIMARY_CONSTRUCTOR_OF_INLINE_CLASS", "DEPRECATION")
inline class Option<out T>
@PublishedApi
@Deprecated("Don't use this constructor directly")
internal constructor(
        @PublishedApi
        @Deprecated("Don't use inner value directly")
        internal val value: Any?
) {
    val isSome: Boolean get() = value !is NoneValue
    val isNone: Boolean get() = value is NoneValue
    inline fun onSome(action: (T)->Unit): Option<T> {
        if (this.isSome) {
            @Suppress("UNCHECKED_CAST")
            action(this.value as T)
        }
        return this
    }
    inline fun onNone(action: ()->Unit): Option<T> {
        if (this.isNone) {
            action()
        }
        return this
    }
    @Suppress("UNCHECKED_CAST")
    inline fun<U> map(f: (T)->U): Option<U> {
        return if (this.isSome) {
            Some(f(this.value as T))
        } else {
            None()
        }
    }

    override fun toString(): String = if(this.isSome) {
        "Some(${this.value})"
    } else {
        "None"
    }

    @PublishedApi
    @Deprecated("Consider this private")
    internal companion object {
        object NoneValue
    }
}

@Suppress("DEPRECATION", "FunctionName")
fun <T>None(): Option<T> = Option(Option.Companion.NoneValue)
@Suppress("DEPRECATION", "FunctionName")
fun <T>Some(value: T): Option<T> = Option(value)

fun<T: Any> T?.toOption(): Option<T> = this?.let { Some(it) }?:None()
