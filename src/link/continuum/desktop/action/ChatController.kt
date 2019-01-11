package link.continuum.desktop.action

import com.github.kittinunf.result.Result
import koma.controller.events.processEventsResult
import koma.controller.sync.MatrixSyncReceiver
import koma.koma_app.SaveToDiskTasks
import koma.koma_app.appState
import koma.matrix.MatrixApi
import koma.matrix.UserId
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
        val apiClient: MatrixApi,
        private val user: UserId,
        full_sync: Boolean = false
) {
    private val sync: MatrixSyncReceiver
    private val data = appState.koma.paths
    init{
        val batch_key = if (full_sync) null  else data.loadSyncBatchToken(user)
        sync = MatrixSyncReceiver(apiClient, batch_key)

        appState.stopSync = {
            logger.debug { "Stopping sync" }
            runBlocking {
                sync.stopSyncing()
            }
            logger.debug { "Sync stopped" }
        }
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
}
