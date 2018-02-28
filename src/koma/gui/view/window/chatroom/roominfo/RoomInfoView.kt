package koma.gui.view.window.chatroom.roominfo

import de.jensd.fx.glyphs.materialicons.MaterialIcon
import de.jensd.fx.glyphs.materialicons.utils.MaterialIconFactory
import javafx.geometry.Pos
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox
import koma.gui.element.icon.AvatarAlways
import koma.gui.view.window.chatroom.roominfo.about.RoomAliasForm
import koma.gui.view.window.chatroom.roominfo.about.requests.chooseUpdateRoomIcon
import koma.gui.view.window.chatroom.roominfo.about.requests.requestUpdateRoomName
import model.Room
import tornadofx.*

class RoomInfoDialog(room: Room): Fragment() {
    override val root= VBox(10.0)

    init {
        this.title = "Update Info of Room ${room.displayName.value}"
        val aliasDialog = RoomAliasForm(room)
        with(root) {
            paddingAll = 5
            hbox(5) {
                vbox(5) {
                    fieldset("Name") {
                        hbox(5) {
                            val input = textfield(room.name.value)
                            button("Set") {
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
