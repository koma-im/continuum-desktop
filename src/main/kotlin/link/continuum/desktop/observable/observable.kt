package link.continuum.desktop.observable

import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow

// experimental

open class Observable<T>() {
    protected val channel = ConflatedBroadcastChannel<T>()
    fun get() = channel.value
    fun getOrNull() = channel.valueOrNull
    fun flow(): Flow<T> {
        val f= flow<T> {
            val s = channel.openSubscription()
            try {
                emitAll(s)
            } finally {
                s.cancel()
            }
        }
        return f
    }
}

class MutableObservable<T>(): Observable<T>() {
    constructor(value: T) : this() {
        channel.offer(value)
    }
    fun set(value: T) {
        channel.offer(value)
    }
    fun close() {
        channel.close()
    }
}