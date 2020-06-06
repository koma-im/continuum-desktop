package link.continuum.desktop.observable

import kotlinx.coroutines.flow.*

// experimental

inline class Observable<T: Any>(
        private val channel: MutableStateFlow<T?> = MutableStateFlow<T?>(null)
) {
    val value: T?
        get() = channel.value
    fun get(): T = channel.value as T
    fun getOrNull() = channel.value
    fun flow(): Flow<T> {
        val f = flow {
            var started = false
            channel.collect {
                if (!started) {
                    started = true
                    if (it == null) return@collect
                }
                emit(it!!)
            }
        }
        return f
    }
    fun set(value: T) {
        channel.value = value
    }
}

typealias  MutableObservable<T> = Observable<T>

fun<T> MutableStateFlow<T>.set(value: T) {
    this.value = value
}
