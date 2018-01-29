package koma.controller.sync

import koma.controller.events_processing.processEventsResult
import koma.matrix.sync.SyncResponse
import koma_app.appState.apiClient
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.javafx.JavaFx
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.selects.select
import ru.gildor.coroutines.retrofit.Result
import ru.gildor.coroutines.retrofit.awaitResult
import java.net.SocketTimeoutException
import java.time.Instant
import java.util.concurrent.TimeUnit

val longPollTimeout = 50

/**
 * if time flows at unusual rates, connection is unlikely to survive
 */
fun detectTimeLeap() = async {
    var prev = Instant.now().epochSecond
    while (true) {
        delay(1, TimeUnit.SECONDS)
        val now = Instant.now().epochSecond
        if (now - prev> 2) {
            println("detected time leap from $prev to $now")
            break
        } else {
            prev = now
        }
    }
}

fun startSyncing(from: String?): Job {
    var since = from
    val client = apiClient!!
    return launch(JavaFx) {
         sync@ while (true) {
             val ar = async {  client.getEvents(since).awaitResult() }
             val tl = detectTimeLeap()
             val resultOr = select<Result<SyncResponse>?> {
                 ar.onAwait { sr -> sr }
                 tl.onAwait { null }
             }
             val eventResult = if (resultOr == null) {
                 println("restart sync")
                 ar.cancel()
                 continue@sync
             } else {
                 tl.cancel()
                 resultOr
             }

            when (eventResult) {
                is Result.Ok -> {
                    val r = eventResult.value
                    processEventsResult(r)
                    since = r.next_batch
                    client.next_batch = since
                }
                is Result.Error -> {
                    val error = "http error ${eventResult.exception.code()}: ${eventResult.exception.message()}"
                    println("syncing $error")
                    delay(500)
                }
                is Result.Exception -> {
                    val ex = eventResult.exception
                    if (ex is SocketTimeoutException) continue@sync
                    println("syncing $ex, ${ex.cause}")
                    delay(500)
                }
            }
        }
    }
}

