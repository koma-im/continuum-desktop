package koma.controller.sync

import koma.controller.events_processing.processEventsResult
import koma_app.appState.apiClient
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.javafx.JavaFx
import kotlinx.coroutines.experimental.launch
import ru.gildor.coroutines.retrofit.Result
import ru.gildor.coroutines.retrofit.awaitResult

fun startSyncing(from: String?) {
    var since = from
    launch(JavaFx) {
        while (true) {
            val eventResult = apiClient!!.getEvents(since).awaitResult()
            when (eventResult) {
                is Result.Ok -> {
                    val r = eventResult.value
                    processEventsResult(r, since != null)
                    since = r.next_batch
                }
                else -> delay(500)
            }
        }
    }
}

