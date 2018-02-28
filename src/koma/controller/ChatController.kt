package controller

import domain.AvatarUrl
import domain.EmptyResult
import javafx.concurrent.Task
import javafx.scene.control.TextInputDialog
import javafx.stage.FileChooser
import koma.controller.events_processing.processEventsResult
import koma.controller.sync.startSyncing
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.javafx.JavaFx
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.runBlocking
import matrix.ApiClient
import rx.lang.kotlin.filterNotNull
import rx.schedulers.Schedulers
import tornadofx.*

/**
 * Created by developer on 2017/6/22.
 */
class ChatController(
        val apiClient: ApiClient) {

    private val shutdownSignalChan = Channel<Unit>()

    init{
        guiEvents.updateAvatar.toObservable()
                .map {
                    val dialog = FileChooser()
                    dialog.title = "Upload a file"

                    val file = dialog.showOpenDialog(FX.primaryStage)
                    file?.absolutePath
                }
                .filterNotNull()
                .observeOn(Schedulers.io())
                .map({
                    val result = apiClient.uploadMedia(it)
                    result?.content_uri
                })
                .filterNotNull()
                .subscribe {
                    apiClient.updateAvatar(apiClient.userId, AvatarUrl(it))
                }

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

    fun updateMyAlias() {
        val dia = TextInputDialog()
        dia.title = "Update my alias"
        val result = dia.showAndWait()
        val newname = if (result.isPresent && result.get().isNotBlank())
            result.get()
        else
            return
        val task = object: Task<EmptyResult?>() {
            override fun call(): EmptyResult? {
                return apiClient.updateDisplayName(newname)
            }
        }
        Thread(task).start()
    }

}
