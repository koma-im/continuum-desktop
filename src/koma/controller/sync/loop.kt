package koma.controller.sync

import koma.controller.events_processing.processEventsResult
import koma_app.appState.apiClient
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.javafx.JavaFx
import kotlinx.coroutines.experimental.launch
import ru.gildor.coroutines.retrofit.Result
import ru.gildor.coroutines.retrofit.awaitResult
import java.net.SocketTimeoutException

fun startSyncing(from: String?): Job {
    var since = from
    return launch(JavaFx) {
         sync@ while (true) {
            val eventResult = apiClient!!.getEvents(since).awaitResult()
            when (eventResult) {
                is Result.Ok -> {
                    val r = eventResult.value
                    processEventsResult(r)
                    since = r.next_batch
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

