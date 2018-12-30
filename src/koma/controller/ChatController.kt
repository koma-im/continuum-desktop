package controller

import com.github.kittinunf.result.Result
import koma.controller.events_processing.processEventsResult
import koma.controller.sync.MatrixSyncReceiver
import koma_app.appState
import kotlinx.coroutines.*
import kotlinx.coroutines.javafx.JavaFx
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

    @ObsoleteCoroutinesApi
    fun start() {
        val initialized = appState.accountRooms()?.isNotEmpty() == true
        val start = if (initialized) apiClient.next_batch else null
        sync.since = start
        GlobalScope.launch(Dispatchers.JavaFx) {
            for (s in sync.events) {
                if (s is Result.Success) {
                    apiClient.profile.processEventsResult(s.value)
                } else if (s is Result.Failure) {
                    // TODO show a retry button for retrying
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
