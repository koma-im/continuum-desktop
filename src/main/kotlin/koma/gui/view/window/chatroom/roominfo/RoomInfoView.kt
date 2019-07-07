package koma.gui.view.window.chatroom.roominfo

import de.jensd.fx.glyphs.materialicons.MaterialIcon
import de.jensd.fx.glyphs.materialicons.utils.MaterialIconFactory
import javafx.beans.property.SimpleBooleanProperty
import javafx.geometry.Pos
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox
import koma.Koma
import koma.gui.element.icon.AvatarAlways
import koma.gui.view.window.chatroom.roominfo.about.RoomAliasForm
import koma.gui.view.window.chatroom.roominfo.about.requests.chooseUpdateRoomIcon
import koma.gui.view.window.chatroom.roominfo.about.requests.requestUpdateRoomName
import koma.matrix.UserId
import koma.matrix.event.room_message.RoomEventType
import link.continuum.database.KDataStore
import link.continuum.database.models.getChangeStateAllowed
import link.continuum.libutil.getOrNull
import model.Room
import okhttp3.OkHttpClient
import tornadofx.*

class RoomInfoDialog(
        room: Room, user: UserId,
        data: KDataStore,
        koma: Koma
): Fragment() {
    override val root= VBox(10.0)
    private val roomicon = AvatarAlways(koma)

    init {
        val avatarUrl = objectBinding(room.avatar) {value?.getOrNull()}
        roomicon.bind(room.displayName, room.color, avatarUrl, room.server)
        val canEditName = getChangeStateAllowed(data, room.id, user, RoomEventType.Name.toString())
        val canEditAvatar = getChangeStateAllowed(data, room.id, user, RoomEventType.Avatar.toString())

        this.title = "Update Info of Room ${room.displayName.value}"
        val aliasDialog = RoomAliasForm(room, user, data)
        with(root) {
            paddingAll = 5
            hbox(5) {
                vbox(5) {
                    fieldset("Name") {
                        hbox(5) {
                            val input = textfield(room.name.value?.getOrNull())
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
