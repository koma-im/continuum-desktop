package koma.gui.view.window.chatroom.roominfo

import de.jensd.fx.glyphs.materialicons.MaterialIcon
import de.jensd.fx.glyphs.materialicons.utils.MaterialIconFactory
import javafx.beans.property.SimpleBooleanProperty
import javafx.geometry.Pos
import javafx.scene.Scene
import javafx.scene.control.Hyperlink
import javafx.scene.control.TextField
import javafx.scene.layout.Priority
import javafx.stage.Stage
import javafx.stage.Window
import koma.gui.element.icon.AvatarAlways
import koma.gui.view.window.chatroom.roominfo.about.RoomAliasForm
import koma.gui.view.window.chatroom.roominfo.about.requests.chooseUpdateRoomIcon
import koma.gui.view.window.chatroom.roominfo.about.requests.requestUpdateRoomName
import koma.matrix.UserId
import koma.matrix.event.room_message.RoomEventType
import link.continuum.database.KDataStore
import link.continuum.database.models.getChangeStateAllowed
import link.continuum.desktop.gui.*
import link.continuum.desktop.util.getOrNull
import link.continuum.desktop.Room

class RoomInfoDialog(
        room: Room, user: UserId,
        data: KDataStore
) {
    val root= VBox(10.0)
    private val roomicon = AvatarAlways()
    val stage = Stage()

    fun openWindow(owner: Window) {
        stage.initOwner(owner)
        stage.scene = Scene(root)
        stage.show()
    }
    init {
        val avatarUrl = objectBinding(room.avatar) {value?.getOrNull()}
        roomicon.bind(room.displayName, room.color, avatarUrl, room.account.server)
        val canEditName = getChangeStateAllowed(data, room.id, user, RoomEventType.Name.toString())
        val canEditAvatar = getChangeStateAllowed(data, room.id, user, RoomEventType.Avatar.toString())

        stage.title = "Update Info of Room ${room.displayName.value}"
        val aliasDialog = RoomAliasForm(room, user, data)
        with(root) {
            padding = UiConstants.insets5
            hbox(5.0) {
                vbox {
                    spacing = 5.0
                        hbox(5.0) {
                            alignment = Pos.CENTER
                            text("Name")
                            val input = TextField(room.name.value?.getOrNull())
                            input.editableProperty().value = canEditName
                            add(input)
                            button("Set") {
                                removeWhen(SimpleBooleanProperty(canEditName).not())
                                action {
                                    requestUpdateRoomName(room, input.text)
                                }
                            }
                        }
                }
                hbox { HBox.setHgrow(this, Priority.ALWAYS) }
                vbox() {
                    spacing = 5.0
                    padding = UiConstants.insets5
                    alignment = Pos.CENTER
                    add(roomicon)
                    val camera = MaterialIconFactory.get().createIcon(MaterialIcon.PHOTO_CAMERA)
                    add(Hyperlink().apply {
                        graphic = camera
                        removeWhen(SimpleBooleanProperty(canEditAvatar).not())
                        setOnAction {
                            chooseUpdateRoomIcon(room)
                        }
                    })
                }
            }
            add(aliasDialog.root)
        }
    }
}
