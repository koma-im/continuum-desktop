package view.dialog

import koma_app.appState
import domain.DiscoveredRoom
import javafx.beans.property.StringProperty
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.scene.control.ButtonBar
import javafx.scene.control.ButtonType
import javafx.scene.control.Dialog
import rx.Observable
import rx.javafx.kt.observeOnFx
import rx.javafx.kt.toObservable
import rx.lang.kotlin.filterNotNull
import rx.schedulers.Schedulers
import tornadofx.*

/**
 * Created by developer on 2017/7/9.
 */
class FindJoinRoomDialog: Dialog<String>() {

    val publicRoomList: ObservableList<DiscoveredRoom> = FXCollections.observableArrayList<DiscoveredRoom>()

    lateinit var roomfield: StringProperty

    init {
        this.setTitle("Room finder")
        this.setHeaderText("Join a room")

        val joinButtonType = ButtonType("Join", ButtonBar.ButtonData.OK_DONE)
        this.getDialogPane().getButtonTypes().addAll(joinButtonType, ButtonType.CANCEL)

        val joinButton = this.getDialogPane().lookupButton(joinButtonType)
        joinButton.setDisable(true)

        val grid = vbox {
            hbox {
                label("Room")
                textfield() {
                    roomfield = textProperty()
                    textProperty().addListener({ observable, oldValue, newValue ->
                        joinButton.setDisable(newValue.trim().isEmpty()) })
                }
            }
            tableview(publicRoomList) {
                column("Name", DiscoveredRoom::aliases) {

                }
                column("Members", DiscoveredRoom::num_joined_members) {

                }
                selectionModel.selectedItemProperty().toObservable()
                        .filterNotNull() // when nothing's selected
                        .subscribe {
                            roomfield.set(it.room_id)
                        }
            }
        }
        this.getDialogPane().setContent(grid)

        this.setResultConverter({ dialogButton ->
            if (dialogButton === joinButtonType) {
                return@setResultConverter roomfield.get()
            }
            null
        })

        loadPublicRooms()
    }

    private fun loadPublicRooms() {
        Observable.just(Unit).observeOn(Schedulers.io())
                .map { appState.apiClient?.service?.publicRooms()?.execute() }
                .map { it?.body()?.chunk }
                .filterNotNull()
                .observeOnFx()
                .subscribe {
                    publicRoomList.addAll(it)
                }
    }
}
