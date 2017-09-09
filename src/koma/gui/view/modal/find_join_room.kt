package view.dialog

import domain.DiscoveredRoom
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.scene.control.ButtonBar
import javafx.scene.control.ButtonType
import javafx.scene.control.Dialog
import koma.concurrency.LoadPublicRoomsService
import koma.gui.view.listview.room.discovery.PublicRoomsView

/**
 * Created by developer on 2017/7/9.
 */
class FindJoinRoomDialog: Dialog<String>() {

    private val publicRoomList: ObservableList<DiscoveredRoom> = FXCollections.observableArrayList<DiscoveredRoom>()


    init {
        this.isResizable = true
        this.setTitle("Room finder")
        this.setHeaderText("Join a room")

        val joinButtonType = ButtonType("Join", ButtonBar.ButtonData.OK_DONE)
        this.getDialogPane().getButtonTypes().addAll(joinButtonType, ButtonType.CANCEL)

        val joinButton = this.getDialogPane().lookupButton(joinButtonType)
        joinButton.setDisable(true)

        val pubs = PublicRoomsView(publicRoomList, joinButton)
        this.getDialogPane().setContent(pubs.ui)

        this.setResultConverter({ dialogButton ->
            if (dialogButton === joinButtonType) {
                return@setResultConverter pubs.roomfield.get()
            }
            null
        })

        loadPublicRooms()
    }

    private fun loadPublicRooms() {
        val serv = LoadPublicRoomsService()
        serv.setOnSucceeded {
            val value = serv.value
            val rooms = value.chunk
            publicRoomList.addAll(rooms)
        }
        serv.start()
    }
}
