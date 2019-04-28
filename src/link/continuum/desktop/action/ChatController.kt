package link.continuum.desktop.action

import com.github.kittinunf.result.Result
import koma.controller.events.processEventsResult
import koma.controller.sync.MatrixSyncReceiver
import koma.gui.view.SyncStatusBar
import koma.koma_app.appState
import koma.matrix.MatrixApi
import koma.matrix.UserId
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.javafx.JavaFx
import link.continuum.database.KDataStore
import link.continuum.database.models.getSyncBatchKey
import link.continuum.database.models.saveSyncBatchKey
import link.continuum.desktop.gui.list.user.UserDataStore
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
        private val data: KDataStore,
        private val userDataStore: UserDataStore,
        full_sync: Boolean = false
) {
    private val sync: MatrixSyncReceiver

    init{
        val batch_key = if (full_sync) {
            GlobalScope.launch {
                statusChan.send(SyncStatusBar.Variants.FullSync())
            }
            null
        }  else {
            getSyncBatchKey(data, user)
        }
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
                    processEventsResult(user, s.value, userDataStore, apiClient.server)
                    val nb = sync.since
                    nb?.let {
                        saveSyncBatchKey(data, user, nb)
                    }
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
    }

    fun start() {
        sync.startSyncing()
    }
}