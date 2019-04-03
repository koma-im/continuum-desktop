package koma.gui.view.window.roomfinder.publicroomlist.listcell

import com.github.kittinunf.result.failure
import com.github.kittinunf.result.success
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.event.EventTarget
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.control.ListCell
import javafx.scene.layout.Priority
import javafx.scene.paint.Color
import javafx.scene.text.FontWeight
import koma.controller.events.addJoinedRoom
import koma.gui.element.icon.AvatarAlways
import koma.gui.element.icon.placeholder.generator.hashStringColorDark
import koma.koma_app.appState
import koma.matrix.DiscoveredRoom
import koma.matrix.room.naming.RoomId
import koma.network.media.MHUrl
import koma.util.coroutine.adapter.retrofit.awaitMatrix
import koma.util.result.ok
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.javafx.JavaFx
import kotlinx.coroutines.launch
import mu.KotlinLogging
import okhttp3.HttpUrl
import org.controlsfx.control.Notifications
import tornadofx.*

private val logger = KotlinLogging.logger {}

class DiscoveredRoomFragment(private val server: HttpUrl): ListCell<DiscoveredRoom>() {
    val root = hbox(spacing = 10.0)
    val avUrl = SimpleObjectProperty<HttpUrl>()
    val name = SimpleStringProperty()
    val avColor = SimpleObjectProperty<Color>()
    val worldRead = SimpleBooleanProperty(false)
    val guestJoin = SimpleBooleanProperty(false)
    val members = SimpleIntegerProperty(-1)
    private val topic = SimpleStringProperty("")
    private val roomId =  SimpleObjectProperty<RoomId>()
    private val aliasesLabel = label() {
        style {
            opacity = 0.6
        }
    }

    override fun updateItem(item: DiscoveredRoom?, empty: Boolean) {
        logger.debug { "discovered room $item, empty=$empty" }
        super.updateItem(item, empty)
        if (empty || item == null) {
            text = null
            graphic = null
            return
        }
        avColor.set(hashStringColorDark(item.room_id.id))
        val avU = item.avatar_url?.let { MHUrl.fromStr(it).ok() }?.toHttpUrl(server)?.ok()
        avU ?.let { avUrl.set(avU) }
        name.set(item.dispName())
        worldRead.set(item.world_readable)
        guestJoin.set(item.guest_can_join)
        members.set(item.num_joined_members)
        item.topic?.let { topic.set(it) }
        item.aliases?.let {
            aliasesLabel.text = it.joinToString(", ")
        }
        roomId.set(item.room_id)
        graphic = root
    }
    init {
        with(root) { setUpCell() }
    }

    private fun EventTarget.setUpCell(){
        add(AvatarAlways(avUrl, name, avColor))
        stackpane {
            hgrow = Priority.ALWAYS
            vgrow = Priority.ALWAYS
            vbox {
                minWidth = 1.0
                prefWidth = 1.0
                hgrow = Priority.ALWAYS
                vgrow = Priority.ALWAYS
                hbox(9.0) {
                    label(name) {
                        style {
                            fontWeight = FontWeight.EXTRA_BOLD
                        }
                    }
                    label("World Readable") { removeWhen { worldRead.not() } }
                    label("Guest Joinable") { removeWhen { guestJoin.not() } }
                    text("Members: ") { style { opacity = 0.5 } }
                    text() {
                        textProperty().bind(stringBinding(members) { "$value" })
                    }
                }
                val topicLess = booleanBinding(topic) { value?.isEmpty() ?: true }
                text(topic) { removeWhen { topicLess } }
                add(aliasesLabel)
            }
            stackpane {
                val h = hoverProperty()
                button("Join") {
                    visibleWhen { h }
                    action { joinById(roomId.value, name.value, root) }
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
