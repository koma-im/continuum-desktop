package controller

import com.github.kittinunf.result.Result
import koma.controller.events_processing.processEventsResult
import koma.controller.sync.MatrixSyncReceiver
import kotlinx.coroutines.*
import kotlinx.coroutines.javafx.JavaFx
import matrix.ApiClient
import mu.KotlinLogging
import java.net.SocketTimeoutException

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
                } else if (s is Result.Failure) {
                    if (s.error is SocketTimeoutException) {
                        logger.warn { "sync paused after timeout exception" }
                        delay(1500)
                        logger.info { "resuming sync after timeout" }
                    } else {
                        logger.warn { "sync stopped because of $s" }
                    }
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
