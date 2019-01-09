package koma.gui.view.window.roomfinder.publicroomlist.listcell

import com.github.kittinunf.result.failure
import com.github.kittinunf.result.success
import javafx.beans.property.ObjectProperty
import javafx.beans.property.SimpleListProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.collections.FXCollections
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.layout.Priority
import javafx.scene.paint.Color
import javafx.scene.text.FontWeight
import koma.controller.events.addJoinedRoom
import koma.gui.element.icon.AvatarAlways
import koma.gui.element.icon.placeholder.generator.hashStringColorDark
import koma.koma_app.appState
import koma.matrix.DiscoveredRoom
import koma.matrix.room.naming.RoomId
import koma.util.coroutine.adapter.retrofit.awaitMatrix
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.javafx.JavaFx
import kotlinx.coroutines.launch
import org.controlsfx.control.Notifications
import tornadofx.*

private fun<E> listToProperty(list: List<E>): SimpleListProperty<E> {
    val l = list
    return SimpleListProperty(FXCollections.observableArrayList(l))
}

class DiscoveredRoomItemModel(property: ObjectProperty<DiscoveredRoom>)
    : ItemViewModel<DiscoveredRoom>(itemProperty = property) {
    val name = bind {item?.name.toProperty()}
    val aliases = bind {item?.aliases?.let { listToProperty(it) } }
    val displayName = bind { item?.dispName().toProperty() }
    val avatar_url = bind {item?.avatar_url.toProperty()}
    val guest = bind {item?.guest_can_join.toProperty()}
    val n_mems = bind {item?.num_joined_members.toProperty()}
    val room_id = bind {item?.room_id.toProperty()}
    val topic = bind {item?.topic.toProperty()}
    val world_read = bind {item?.world_readable.toProperty()}
}

class DiscoveredRoomFragment: ListCellFragment<DiscoveredRoom>() {
    override val root = hbox(spacing = 10.0)
    val droom = DiscoveredRoomItemModel(itemProperty)
    private val avatar: AvatarAlways

    init {
        val color = SimpleObjectProperty<Color>()
        color.bind(objectBinding(droom.room_id) { value ?.let { hashStringColorDark( it.id)}} )
        avatar = AvatarAlways(droom.avatar_url, droom.displayName, color)
        with(root) { setUpCell() }
    }

    private fun setUpCell(){
        add(avatar)
        stackpane {
            hgrow = Priority.ALWAYS
            vgrow = Priority.ALWAYS
            vbox {
                minWidth = 1.0
                prefWidth = 1.0
                hgrow = Priority.ALWAYS
                vgrow = Priority.ALWAYS
                hbox(9.0) {
                    label(droom.displayName) {
                        style {
                            fontWeight = FontWeight.EXTRA_BOLD
                        }
                    }
                    label("World Readable") { removeWhen { droom.world_read.toBinding().not() } }
                    label("Guest Joinable") { removeWhen { droom.guest.toBinding().not() } }
                    text("Members: ") { style { opacity = 0.5 } }
                    text() {
                        textProperty().bind(stringBinding(droom.n_mems) { "$value" })
                    }
                }
                val topicLess = booleanBinding(droom.topic) { value?.isEmpty() ?: true }
                text(droom.topic) { removeWhen { topicLess } }
                val aliases = stringBinding(droom.aliases) { value?.joinToString(", ") }
                label() {
                    textProperty().bind(aliases)
                    style {
                        opacity = 0.6
                    }
                }
            }
            stackpane {
                val h = hoverProperty()
                button("Join") {
                    visibleWhen { h }
                    action { joinById(droom.room_id.value, droom.displayName.value, root) }
                }
                alignment = Pos.CENTER_RIGHT
            }
        }
    }
}

fun joinById(roomid: RoomId, name: String, owner: Node) {
    val api = appState.apiClient
    api ?: return
    GlobalScope.launch {
        val rs = api.joinRoom(roomid).awaitMatrix()
        rs.success {
            launch(Dispatchers.JavaFx) { addJoinedRoom(api.userId, roomid) }
        }
        rs.failure {
            launch(Dispatchers.JavaFx) {
                Notifications.create()
                        .owner(owner)
                        .title("Failed to join room ${name}")
                        .position(Pos.CENTER)
                        .text(it.message)
                        .showWarning()
            }
        }
    }
}
