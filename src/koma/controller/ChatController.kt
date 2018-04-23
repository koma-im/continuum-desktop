package controller

import koma.controller.events_processing.processEventsResult
import koma.controller.sync.startSyncing
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.javafx.JavaFx
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.runBlocking
import matrix.ApiClient

/**
 * Created by developer on 2017/6/22.
 */
class ChatController(
        val apiClient: ApiClient) {

    private val shutdownSignalChan = Channel<Unit>()

    init{

    }

    fun start() {
        val start = if (apiClient.profile.hasRooms) apiClient.next_batch else null
        val syncEventChannel = startSyncing(start, shutdownSignalChan)
        launch(JavaFx) {
            for (s in syncEventChannel) {
                apiClient.profile.processEventsResult(s)
            }
        }
    }

    fun shutdown() {
        runBlocking {
            shutdownSignalChan.send(Unit)
            shutdownSignalChan.receive()
        }
    }
}
