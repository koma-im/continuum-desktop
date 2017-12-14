package controller

import domain.AvatarUrl
import domain.EmptyResult
import javafx.concurrent.Task
import javafx.scene.control.*
import javafx.scene.layout.GridPane
import javafx.stage.FileChooser
import koma.controller.sync.startSyncing
import kotlinx.coroutines.experimental.Job
import matrix.ApiClient
import rx.lang.kotlin.filterNotNull
import rx.lang.kotlin.subscribeBy
import rx.schedulers.Schedulers
import tornadofx.*
import java.util.*

/**
 * Created by developer on 2017/6/22.
 */
class ChatController(
        val apiClient: ApiClient) {

    private val syncJob: Job

    init{

        guiEvents.createRoomRequests.toObservable()
                .map{ createRoom() }
                .observeOn(Schedulers.io())
                .filter{it.isPresent}
                .subscribe {
                    val room_publicity: Pair<String, Boolean> = it.get()
                    val roomname = room_publicity.first
                    val visibility = if (room_publicity.second) "public" else "private"

                    val result = apiClient.createRoom(roomname, visibility)
                    if (result == null) {
                        println("Failed to create $visibility room $roomname")
                    } else {
                        println("created room $result")
                    }
                }
        guiEvents.leaveRoomRequests.toObservable()
                .map { it.id }
                .observeOn(Schedulers.io())
                .subscribeBy(onNext = {
                    apiClient.leavingRoom(it)
                })
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
        guiEvents.uploadRoomIconRequests.toObservable()
                .map {
                    val dialog = FileChooser()
                    dialog.title = "Upload a icon for the room"

                    val file = dialog.showOpenDialog(FX.primaryStage)
                    Pair(it, file)
                }
                .observeOn(Schedulers.io())
                .subscribe {
                    val room = it.first
                    val file = it.second
                    if (file != null && file.absolutePath != null) {
                        val result = apiClient.uploadMedia(file.absolutePath)
                        if (result != null)
                            apiClient.uploadRoomIcon(room.id, result.content_uri)
                    }
                }
        guiEvents.sendImageRequests.toObservable()
                .map {
                    val dialog = FileChooser()
                    dialog.title = "Send an image"

                    val file = dialog.showOpenDialog(FX.primaryStage)
                    Pair(it, file)
                }
                .observeOn(Schedulers.io())
                .subscribe {
                    val room = it.first
                    val file = it.second
                    if (file != null && file.absolutePath != null) {
                        val result = apiClient.uploadMedia(file.absolutePath)
                        if (result != null)
                            apiClient.sendImage(room.id, result.content_uri, file.name)
                    }
                }
        guiEvents.putRoomAliasRequests.toObservable()
                .map {
                    val dia = TextInputDialog()
                    dia.title = "Add alias to room ${it.displayName}"
                    val result = dia.showAndWait()
                    val newname = if (result.isPresent && result.get().isNotBlank())
                       result.get()
                    else
                        null
                    Pair(it, newname)
                }
                .filter { it.second != null }
                .observeOn(Schedulers.io())
                .subscribe {
                    val roomid = it.first.id
                    val newname = it.second as String
                    val roomAlias = apiClient.setRoomAlias(roomid, newname)
                }
        guiEvents.renameRoomRequests.toObservable()
                .map {
                    val dia = TextInputDialog()
                    dia.title = "Rename room ${it.displayName}"
                    val result = dia.showAndWait()
                    val newname = if (result.isPresent && result.get().isNotBlank())
                       result.get()
                    else
                        null
                    Pair(it, newname)
                }
                .filter { it.second != null }
                .observeOn(Schedulers.io())
                .subscribe {
                    val roomid = it.first.id
                    val newname = it.second as String
                    apiClient.setRoomAlias(roomid, newname)
                    apiClient.setRoomCanonicalAlias(roomid, newname)
                }

        syncJob = startSyncing(null)
    }


    fun shutdown() {
        syncJob.cancel()
        apiClient.shutdown()
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

    fun createRoom(): Optional<Pair<String, Boolean>> {
        val dialog: Dialog<Pair<String, Boolean>> = Dialog()
        dialog.setTitle("Creation Dialog")
        dialog.setHeaderText("Create a room")

        val createButtonType = ButtonType("Create", ButtonBar.ButtonData.OK_DONE)
        dialog.getDialogPane().getButtonTypes().addAll(createButtonType, ButtonType.CANCEL)

        val grid = GridPane()

        val roomnamef = TextField()
        roomnamef.setPromptText("Room")
        val publicf = ToggleButton("public")

        grid.add(Label("Room:"), 0, 0)
        grid.add(roomnamef, 1, 0)
        grid.add(publicf, 1, 1)

        val creationButton = dialog.getDialogPane().lookupButton(createButtonType)
        creationButton.setDisable(true)

        roomnamef.textProperty().addListener({ observable, oldValue, newValue ->
            creationButton.setDisable(newValue.trim().isEmpty()) })

        dialog.getDialogPane().setContent(grid)

        dialog.setResultConverter({ dialogButton ->
            if (dialogButton === createButtonType) {
                return@setResultConverter Pair(roomnamef.getText(), publicf.isSelected)
            }
            null
        })

        val result = dialog.showAndWait()

        return result
    }


}
