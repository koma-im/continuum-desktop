package koma.gui.view.window.chatroom.roominfo

import de.jensd.fx.glyphs.materialicons.MaterialIcon
import de.jensd.fx.glyphs.materialicons.utils.MaterialIconFactory
import javafx.beans.property.SimpleBooleanProperty
import javafx.geometry.Pos
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox
import koma.gui.element.icon.AvatarAlways
import koma.gui.view.window.chatroom.roominfo.about.RoomAliasForm
import koma.gui.view.window.chatroom.roominfo.about.requests.chooseUpdateRoomIcon
import koma.gui.view.window.chatroom.roominfo.about.requests.requestUpdateRoomName
import koma.matrix.UserId
import koma.matrix.room.power.canUserSet
import matrix.event.room_message.RoomEventType
import model.Room
import tornadofx.*

class RoomInfoDialog(room: Room, user: UserId): Fragment() {
    override val root= VBox(10.0)

    init {
        val canEditName = room.power_levels.canUserSet(user, RoomEventType.Name)
        val canEditAvatar =room.power_levels.canUserSet(user, RoomEventType.Avatar)

        this.title = "Update Info of Room ${room.displayName.value}"
        val aliasDialog = RoomAliasForm(room, user)
        with(root) {
            paddingAll = 5
            hbox(5) {
                vbox(5) {
                    fieldset("Name") {
                        hbox(5) {
                            val input = textfield(room.name.value)
                            input.editableProperty().value = canEditName
                            button("Set") {
                                removeWhen(SimpleBooleanProperty(canEditName).not())
                                action {
                                    requestUpdateRoomName(room, input.text)
                                }
                            }
                        }
                    }
                }
                hbox { hgrow = Priority.ALWAYS }
                vbox(5.0) {
                    paddingAll = 5
                    alignment = Pos.CENTER
                    val roomicon = AvatarAlways(room.iconURLProperty, room.displayName, room.color)
                    add(roomicon)
                    val camera = MaterialIconFactory.get().createIcon(MaterialIcon.PHOTO_CAMERA)
                    hyperlink(graphic = camera) {
                        removeWhen(SimpleBooleanProperty(canEditAvatar).not())
                        action {
                            chooseUpdateRoomIcon(room)
                        }
                    }
                }
            }
            add(aliasDialog.root)
        }
    }
}
