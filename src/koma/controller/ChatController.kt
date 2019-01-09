package controller

import com.github.kittinunf.result.Result
import koma.controller.events.processEventsResult
import koma.controller.sync.MatrixSyncReceiver
import koma.koma_app.SaveToDiskTasks
import koma.koma_app.appState
import koma.matrix.MatrixApi
import koma.storage.persistence.account.loadSyncBatchToken
import koma.storage.persistence.account.saveSyncBatchToken
import kotlinx.coroutines.*
import kotlinx.coroutines.javafx.JavaFx
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * loads sync pagination token from disk by user id
 * Created by developer on 2017/6/22.
 */
class ChatController(
        val apiClient: MatrixApi) {
    private val sync: MatrixSyncReceiver
    private val data = appState.koma.paths
    private val user = appState.apiClient!!.userId
    init{
        val batch_key = data.loadSyncBatchToken(user)
        sync = MatrixSyncReceiver(apiClient, batch_key)
    }

    @ObsoleteCoroutinesApi
    fun start() {
        GlobalScope.launch(Dispatchers.JavaFx) {
            for (s in sync.events) {
                if (s is Result.Success) {
                    processEventsResult(user, s.value)
                } else if (s is Result.Failure) {
                    // TODO show a retry button for retrying
                    logger.warn { "sync stopped because of $s" }
                }
            }
        }
        SaveToDiskTasks.addJob {
            val nb = sync.since
            logger.debug { "Saving batch key $nb" }
            nb?.let { data.saveSyncBatchToken(user, nb) }
        }

        sync.startSyncing()
    }

    fun shutdown() {
        runBlocking {
            sync.stopSyncing()
        }
    }
}
