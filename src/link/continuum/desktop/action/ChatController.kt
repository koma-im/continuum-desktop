package link.continuum.desktop.action

import com.github.kittinunf.result.Result
import koma.controller.events.processEventsResult
import koma.controller.sync.MatrixSyncReceiver
import koma.gui.view.SyncStatusBar
import koma.koma_app.SaveToDiskTasks
import koma.koma_app.appState
import koma.matrix.MatrixApi
import koma.matrix.UserId
import koma.storage.persistence.account.loadSyncBatchToken
import koma.storage.persistence.account.saveSyncBatchToken
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.javafx.JavaFx
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * loads sync pagination token from disk by user id
 * Created by developer on 2017/6/22.
 */
class SyncControl(
        val apiClient: MatrixApi,
        private val user: UserId,
        /**
         * used to show sync status
         */
        private val statusChan: Channel<SyncStatusBar.Variants>,
        full_sync: Boolean = false
) {
    private val sync: MatrixSyncReceiver
    private val data = appState.koma.paths
    init{
        val batch_key = if (full_sync) {
            GlobalScope.launch {
                statusChan.send(SyncStatusBar.Variants.FullSync())
            }
            null
        }  else data.loadSyncBatchToken(user)
        sync = MatrixSyncReceiver(apiClient, batch_key)

        appState.stopSync = {
            logger.debug { "Stopping sync" }
            runBlocking {
                sync.stopSyncing()
            }
            logger.debug { "Sync stopped" }
        }
        startProcessing()
    }

    @ObsoleteCoroutinesApi
    private fun startProcessing() {
        GlobalScope.launch(Dispatchers.JavaFx) {
            for (s in sync.events) {
                if (s is Result.Success) {
                    statusChan.send(SyncStatusBar.Variants.Normal())
                    processEventsResult(user, s.value)
                } else if (s is Result.Failure) {
                    val deferred = CompletableDeferred<Unit>()
                    statusChan.send(SyncStatusBar.Variants.NeedRetry(s.error, deferred))
                    logger.warn { "sync stopped because of $s" }
                    deferred.await()
                    logger.info { "Retrying sync" }
                    start()
                }
            }
        }
        SaveToDiskTasks.addJob {
            val nb = sync.since
            logger.debug { "Saving batch key $nb" }
            nb?.let { data.saveSyncBatchToken(user, nb) }
        }
    }

    fun start() {
        sync.startSyncing()
    }
}
