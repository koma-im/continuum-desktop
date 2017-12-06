package controller

import com.smith.faktor.EventService
import domain.AvatarUrl
import domain.EmptyResult
import javafx.concurrent.Task
import javafx.scene.control.*
import javafx.scene.layout.GridPane
import javafx.stage.FileChooser
import koma.concurrency.runBanRoomMember
import koma.concurrency.runInviteMember
import koma.concurrency.run_join_romm
import koma.controller.events_processing.processEventsResult
import koma.matrix.room.naming.RoomId
import matrix.ApiClient
import rx.lang.kotlin.filterNotNull
import rx.lang.kotlin.subscribeBy
import rx.schedulers.Schedulers
import tornadofx.*
import view.dialog.FindJoinRoomDialog
import java.util.*

/**
 * Created by developer on 2017/6/22.
 */
class ChatController(
        val apiClient: ApiClient) {

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
        guiEvents.joinRoomRequests.toObservable()
                .subscribeBy(onNext = { joinRoom() })
        guiEvents.leaveRoomRequests.toObservable()
                .map { it.id }
                .observeOn(Schedulers.io())
                .subscribeBy(onNext = {
                    apiClient.leavingRoom(it)
                })
        guiEvents.inviteMemberRequests.toObservable()
                .subscribeBy(onNext = { inviteMember() })
        guiEvents.banMemberRequests.toObservable()
                .subscribeBy(onNext = { banMember() })
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

        startSyncing(null)
    }

    fun startSyncing(from: String?) {
        val eventsService = EventService(apiClient, from)
        eventsService.setOnSucceeded {
            val eventResult = eventsService.value
            if (eventResult != null) {
                processEventsResult(eventResult, from != null)
            }
        }

        eventsService.start()
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

    fun joinRoom() {
        val dialog = FindJoinRoomDialog()

        val result = dialog.showAndWait()

        if (!result.isPresent()) {
            println("no room selected to join")
            return
        } else {
            println("now join room ${result.get()}")
        }
        val roomid = result.get()

        try {
            run_join_romm(apiClient, RoomId(roomid))
        } catch(e: Exception) {
            e.printStackTrace()
            alert(Alert.AlertType.WARNING, "Room joining failed; room might be private")
        }
    }

    fun inviteMember() {
        val dialog: Dialog<Pair<String, String>> = Dialog()
        dialog.setTitle("Invite Dialog")
        dialog.setHeaderText("Invite a friend to a room")

        val inviteButtonType = ButtonType("Login", ButtonBar.ButtonData.OK_DONE)
        dialog.getDialogPane().getButtonTypes().addAll(inviteButtonType, ButtonType.CANCEL)

        val grid = GridPane()
        grid.hgap = 10.0
        grid.vgap = 10.0

        val roomnamef = TextField()
        roomnamef.setPromptText("Room")
        val usernamef = TextField()
        usernamef.promptText = "User"

        grid.add(Label("Room:"), 0, 0)
        grid.add(roomnamef, 1, 0)
        grid.add(Label("user:"), 0, 1)
        grid.add(usernamef, 1, 1)

        // Enable/Disable invite button depending on whether a username was entered.
        val inviteButton = dialog.getDialogPane().lookupButton(inviteButtonType)
        inviteButton.setDisable(true)

        // Do some validation (using the Java 8 lambda syntax).
        roomnamef.textProperty().addListener({ observable, oldValue, newValue ->
            inviteButton.setDisable(newValue.trim().isEmpty()) })

        dialog.getDialogPane().setContent(grid)

        // Convert the result to a username-password-pair when the login button is clicked.
        dialog.setResultConverter({ dialogButton ->
            if (dialogButton === inviteButtonType) {
                return@setResultConverter Pair(roomnamef.getText(), usernamef.getText())
            }
            null
        })

        val result = dialog.showAndWait()

        if (!result.isPresent) {
            return
        }

        val room_user: Pair<String, String> = result.get()
        val username = room_user.second
        val roomid = room_user.first

        val inviteService = runInviteMember(apiClient, RoomId(roomid), username)
        println("Inviting $username to $roomid")
    }

    fun banMember() {
        val dialog: Dialog<Pair<String, String>> = Dialog()
        dialog.setTitle("Ban Dialog")
        dialog.setHeaderText("Ban an enemy from a room")

        val inviteButtonType = ButtonType("Ban", ButtonBar.ButtonData.OK_DONE)
        dialog.getDialogPane().getButtonTypes().addAll(inviteButtonType, ButtonType.CANCEL)

        val grid = GridPane()

        val roomnamef = TextField()
        roomnamef.setPromptText("Room")
        val usernamef = TextField()
        usernamef.promptText = "User"

        grid.add(Label("Room:"), 0, 0)
        grid.add(roomnamef, 1, 0)
        grid.add(Label("user:"), 0, 1)
        grid.add(usernamef, 1, 1)

        val banButton = dialog.getDialogPane().lookupButton(inviteButtonType)
        banButton.setDisable(true)

        roomnamef.textProperty().addListener({ observable, oldValue, newValue ->
            banButton.setDisable(newValue.trim().isEmpty()) })

        dialog.getDialogPane().setContent(grid)

        dialog.setResultConverter({ dialogButton ->
            if (dialogButton === inviteButtonType) {
                return@setResultConverter Pair(roomnamef.getText(), usernamef.getText())
            }
            null
        })

        val result = dialog.showAndWait()

        if (!result.isPresent) {
            return
        }

        val room_user: Pair<String, String> = result.get()
        val roomid = room_user.first
        val username = room_user.second

        val banService = runBanRoomMember(apiClient, RoomId(roomid), username)
        println("Banning $username from $roomid")
    }
}
