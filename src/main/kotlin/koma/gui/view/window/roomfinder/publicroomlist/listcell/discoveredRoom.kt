package koma.gui.view.window.roomfinder.publicroomlist.listcell

import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.control.Label
import javafx.scene.control.ListCell
import javafx.scene.layout.Pane
import javafx.scene.layout.Priority
import koma.koma_app.AppData
import koma.matrix.DiscoveredRoom
import koma.matrix.MatrixApi
import koma.matrix.room.naming.RoomId
import koma.network.media.parseMxc
import kotlinx.coroutines.*
import kotlinx.coroutines.javafx.JavaFx
import link.continuum.desktop.database.hashColor
import link.continuum.desktop.gui.*
import link.continuum.desktop.gui.icon.avatar.Avatar2L
import link.continuum.desktop.util.debugAssert
import mu.KotlinLogging
import org.controlsfx.control.Notifications

private val logger = KotlinLogging.logger {}

@ExperimentalCoroutinesApi
class DiscoveredRoomFragment(
        private val account: MatrixApi,
        private val appData: AppData
): ListCell<DiscoveredRoom>() {
    private val scope = MainScope()
    val root = HBox( 10.0)
    private val avatar = Avatar2L()
    var name = ""

    val worldRead = SimpleBooleanProperty(false)
    val guestJoin = SimpleBooleanProperty(false)
    val members = SimpleIntegerProperty(-1)
    private val topic = SimpleStringProperty("")
    private val roomId =  SimpleObjectProperty<RoomId>()
    private val aliasesLabel = Label().apply {
        opacity = 0.6
    }

    override fun updateItem(item: DiscoveredRoom?, empty: Boolean) {
        super.updateItem(item, empty)
        if (empty || item == null) {
            text = null
            graphic = null
            return
        }
        name = item.dispName()
        avatar.updateName(name, item.room_id.hashColor())
        avatar.updateUrl(item.avatar_url?.parseMxc(), account.server)

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

    private fun Pane.setUpCell(){
        add(avatar.root)
        stackpane {
            HBox.setHgrow(this, Priority.ALWAYS)
            VBox.setVgrow(this, Priority.ALWAYS)
            vbox {
                minWidth = 1.0
                prefWidth = 1.0
                HBox.setHgrow(this, Priority.ALWAYS)
                VBox.setVgrow(this, Priority.ALWAYS)
                hbox(9.0) {
                    label(name) {
                        style = "-fx-font-weight: bold;"
                    }
                    label("World Readable") { removeWhen { worldRead.not() } }
                    label("Guest Joinable") { removeWhen { guestJoin.not() } }
                    text("Members: ") {  opacity = 0.5 }
                    text() {
                        textProperty().bind(stringBinding(members) { "$value" })
                    }
                }
                val topicLess = booleanBinding(topic) { value?.isEmpty() ?: true }

                text() {
                    textProperty().bind(topic)
                    removeWhen { topicLess }
                }
                add(aliasesLabel)
            }
            stackpane {
                val h = hoverProperty()
                button("Join") {
                    visibleWhen(h)
                    action { scope.joinById(roomId.value, name, root, account, appData) }
                }
                alignment = Pos.CENTER_RIGHT
            }
        }
    }
}

fun CoroutineScope.joinById(roomid: RoomId, name: String, owner: Node,
                            api: MatrixApi,
                            store: AppData) {
    launch {
        val (success, failure, _) = api.joinRoom(roomid)
        if (success != null)  {
            val myRooms = store.keyValueStore.roomsOf(api.userId)
            myRooms.join(listOf(roomid))
        } else {
            debugAssert(failure != null)
            launch(Dispatchers.JavaFx) {
                Notifications.create()
                        .owner(owner)
                        .title("Failed to join room ${name}")
                        .position(Pos.CENTER)
                        .text(failure?.message)
                        .showWarning()
            }
        }
    }
}
