package link.continuum.desktop.gui

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.selects.select
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * convert a channel of unordered updates with timestamps
 * to one that provides the latest value
 */
@ExperimentalCoroutinesApi
class UpdateConflater<T> {
    private val inputs = Channel<Pair<Long, T>>()
    private val updates = ConflatedBroadcastChannel<T>()

    suspend fun update(time: Long, value: T) {
        inputs.send(time to value)
    }
    fun subscribe(): ReceiveChannel<T> {
        return updates.openSubscription()
    }

    init {
        GlobalScope.launch { processUpdates() }
    }
    private fun CoroutineScope.processUpdates() {
        var latest = 0L
        var lastValue: T? = null
        launch {
            for ((t, v) in inputs) {
                if (t >= latest) {
                    if (v != lastValue) {
                        logger.trace { "value $v updated at $t, newer than $latest" }
                        latest = t
                        lastValue = v
                        updates.send(v)
                    }
                } else {
                    logger.info { "value $v comes at $t, older than $latest" }
                }
            }
        }
    }
}

fun<T, U, C: SendChannel<U>> CoroutineScope.switchGetDeferred(
        input: ReceiveChannel<T>,
        getDeferred: (T)->Deferred<U>,
        output: C
) {
    launch {
        var current = input.receive()
        while (isActive) {
            val next = select<T?> {
                input.onReceiveOrNull { update ->
                    update
                }
                getDeferred(current).onAwait {
                    output.send(it)
                    input.receiveOrNull()
                }
            }
            if (next == null) {
                logger.debug { "no more input after $current" }
                break
            } else {
                current = next
            }
        }
        logger.debug { "closing switchMapDeferred" }
        input.cancel()
        output.close()
    }
}

/**
 * switch channel by key
 */
@ExperimentalCoroutinesApi
fun <T, U> CoroutineScope.switchUpdates(
        input: ReceiveChannel<T>,
        getUpdates: (T)->ReceiveChannel<U>
): ReceiveChannel<U> {
    val output = Channel<U>(Channel.CONFLATED)
    launch {
        var key = input.receive()
        var updates = getUpdates(key)
        loop@ while (isActive) {
            val state = select<UpdateState<T>> {
                input.onReceiveOrNull { k ->
                    k?.let {
                        logger.debug { "switching to updates for $k" }
                        UpdateState.Switch<T>(it)
                    } ?: UpdateState.Close()
                }
                updates.onReceive {
                    logger.debug { "got update for $key: $it" }
                    output.send(it)
                    UpdateState.Continue()
                }
            }
            when (state) {
                is UpdateState.Switch<T> -> {
                    updates.cancel()
                    key = state.key
                    updates = getUpdates(key)
                }
                is UpdateState.Close -> {
                    logger.debug { "no more updates after $key" }
                    break@loop
                }
                is UpdateState.Continue -> {
                }
            }
        }
        logger.debug { "canceling subscription" }
        input.cancel()
        output.close()
    }
    return output
}

sealed class UpdateState<T> {
    class Switch<T>(val key: T): UpdateState<T>()
    class Close<T>: UpdateState<T>()
    class Continue<T>: UpdateState<T>()
}
