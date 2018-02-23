package koma.controller.sync

import koma.matrix.sync.SyncResponse
import koma_app.appState.chatController
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.selects.select
import ru.gildor.coroutines.retrofit.awaitResult
import java.time.Instant
import java.util.concurrent.TimeUnit

val longPollTimeout = 50

/**
 * if time flows at unusual rates, connection is unlikely to survive
 */
fun detectTimeLeap(): Channel<Unit> {
    val timeleapSignal = Channel<Unit>(Channel.CONFLATED)
    var prev = Instant.now().epochSecond
    launch {
        while (true) {
            delay(1, TimeUnit.SECONDS)
            val now = Instant.now().epochSecond
            if (now - prev > 2) {
                println("detected time leap from $prev to $now")
                timeleapSignal.send(Unit)
            }
            prev = now
        }
    }
    return timeleapSignal
}


fun startSyncing(from: String?, shutdownChan: Channel<Unit>): Channel<SyncResponse> {
    // maybe a little faster with buffer?
    val sresChannel = Channel<SyncResponse>(3)

    var since = from
    val client = chatController.apiClient

    val timeCheck = detectTimeLeap()
    launch {
        sync@ while (true) {
            val ar = async {  client.getEvents(since).awaitResult() }

            val ss = select<SyncStatus> {
                shutdownChan.onReceive { SyncStatus.Shutdown() }
                timeCheck.onReceive { SyncStatus.Resync() }
                ar.onAwait { it.inspect() }
            }
            when (ss) {
                is SyncStatus.Shutdown -> {
                    println("shutting down sync")
                    ar.cancel()
                    sresChannel.close()
                    shutdownChan.send(Unit)
                    break@sync
                }
                is SyncStatus.TransientFailure -> {
                    System.err.println("restarting sync after ${ss.delay} because ${ss.exception}")
                    delay(ss.delay)
                }
                is SyncStatus.Response -> {
                    val r = ss.response
                    sresChannel.send(r)
                    since = r.next_batch
                    client.next_batch = since
                }
                is SyncStatus.Resync -> {
                    System.err.println("restarting sync now because of time leap")
                    ar.cancel()
                }
            }
        }
    }

    return sresChannel
}

