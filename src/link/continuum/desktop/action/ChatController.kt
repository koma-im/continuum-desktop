package link.continuum.desktop.action

import koma.controller.events.processEventsResult
import koma.controller.sync.MatrixSyncReceiver
import koma.gui.view.ChatView
import koma.gui.view.SyncStatusBar
import koma.koma_app.AppStore
import koma.koma_app.appState
import koma.matrix.MatrixApi
import koma.matrix.UserId
import koma.util.onFailure
import koma.util.onSuccess
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.javafx.JavaFx
import link.continuum.database.models.getSyncBatchKey
import link.continuum.database.models.saveSyncBatchKey
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * loads sync pagination token from disk by user id
 * Created by developer on 2017/6/22.
 */
@ExperimentalCoroutinesApi
class SyncControl(
        val apiClient: MatrixApi,
        private val user: UserId,
        /**
         * used to show sync status
         */
        private val statusChan: Channel<SyncStatusBar.Variants>,
        private val appData: AppStore,
        private val view: ChatView,
        coroutineScope: CoroutineScope,
        full_sync: Boolean = false
) {
    private val sync: MatrixSyncReceiver

    init{
        val batch_key = if (full_sync) {
            null
        }  else {
            getSyncBatchKey(appData.database, user)
        }
        sync = MatrixSyncReceiver(apiClient, batch_key)
        coroutineScope.startProcessing()
        coroutineScope.launch {
            sync.startSyncing()
        }
    }

    @ObsoleteCoroutinesApi
    private fun CoroutineScope.startProcessing() {
        launch(Dispatchers.JavaFx) {
            for (s in sync.events) {
                s.onSuccess {
                    statusChan.send(SyncStatusBar.Variants.Normal())
                    processEventsResult(it, apiClient, appData = appData, view = view)
                    val nb = sync.since
                    nb?.let {
                        saveSyncBatchKey(appData.database, user, nb)
                    }
                }.onFailure {
                    statusChan.send(SyncStatusBar.Variants.NeedRetry(it))
                    logger.warn { "sync issue $s" }
                }
            }
            logger.debug { "events are no longer processed" }
        }
    }
}
