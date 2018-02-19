package koma.gui.view.window.roomfinder.publicroomlist.listcell

import domain.DiscoveredRoom
import javafx.beans.property.ObjectProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.scene.layout.Priority
import javafx.scene.paint.Color
import javafx.scene.text.FontWeight
import koma.gui.element.icon.AvatarAlways
import koma.gui.element.icon.placeholder.generator.hashStringColorDark
import tornadofx.*

class DiscoveredRoomItemModel(property: ObjectProperty<DiscoveredRoom>)
    : ItemViewModel<DiscoveredRoom>(itemProperty = property) {
    val name = bind {item?.name.toProperty()}
    val aliases = bind {item?.aliasesProperty()}
    val displayName = bind { item?.dispName().toProperty() }
    val avatar_url = bind {item?.avatar_url.toProperty()}
    val guest = bind {item?.guest_can_join.toProperty()}
    val n_mems = bind {item?.num_joined_members.toProperty()}
    val room_id = bind {item?.room_id.toProperty()}
    val topic = bind {item?.topic.toProperty()}
    val world_read = bind {item?.world_readable.toProperty()}
}

class DiscoveredRoomFragment: ListCellFragment<DiscoveredRoom>() {

    val droom = DiscoveredRoomItemModel(itemProperty)
    private val avatar: AvatarAlways

    init {
        val color = SimpleObjectProperty<Color>()
        color.bind(objectBinding(droom.room_id) { hashStringColorDark( value)} )
        avatar = AvatarAlways(droom.avatar_url, droom.displayName, color)
    }

    override val root = hbox(spacing = 10.0) {
        add(avatar)
        vbox  {
            minWidth = 1.0
            prefWidth = 1.0
            hgrow = Priority.ALWAYS
            vgrow = Priority.ALWAYS
            hbox(9.0) {
                text(droom.displayName) {
                    style {
                        fontWeight = FontWeight.EXTRA_BOLD
                    }
                }
                label("World Readable") { removeWhen { droom.world_read.toBinding().not() }}
                label("Guest Joinable") { removeWhen { droom.guest.toBinding().not() }}
                text("Members: ") { style { opacity = 0.5}}
                text() {
                    textProperty().bind(stringBinding(droom.n_mems) { "$value"})
                }
            }
            val topicLess = booleanBinding(droom.topic) { value?.isEmpty() ?: true }
            text(droom.topic) { removeWhen { topicLess }}
            val aliases = stringBinding(droom.aliases) { value?.joinToString(", ")}
            label() {
                textProperty().bind(aliases)
                style {
                    opacity = 0.6
                }
            }
        }
    }
}
