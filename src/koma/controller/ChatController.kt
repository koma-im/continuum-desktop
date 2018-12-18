package controller

import com.github.kittinunf.result.Result
import koma.controller.events_processing.processEventsResult
import koma.controller.sync.MatrixSyncReceiver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.javafx.JavaFx
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import matrix.ApiClient
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Created by developer on 2017/6/22.
 */
class ChatController(
        val apiClient: ApiClient) {
    private val sync = MatrixSyncReceiver()
    init{

    }

    fun start() {
        val start = if (apiClient.profile.hasRooms) apiClient.next_batch else null
        sync.since = start
        GlobalScope.launch(Dispatchers.JavaFx) {
            for (s in sync.events) {
                if (s is Result.Success) {
                    apiClient.profile.processEventsResult(s.value)
                } else {
                    logger.warn { "sync stopped because of $s" }
                }
            }
        }
        sync.startSyncing()
    }

    fun shutdown() {
        runBlocking {
            sync.stopSyncing()
        }
    }
}
