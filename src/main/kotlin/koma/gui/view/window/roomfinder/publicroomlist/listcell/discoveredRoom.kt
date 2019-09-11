package koma.gui.view.window.roomfinder.publicroomlist.listcell

import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.control.Label
import javafx.scene.control.ListCell
import javafx.scene.image.ImageView
import javafx.scene.layout.HBox
import javafx.scene.layout.Pane
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox
import koma.gui.element.icon.placeholder.generator.hashStringColorDark
import koma.koma_app.AppStore
import koma.koma_app.appState
import koma.matrix.DiscoveredRoom
import koma.matrix.MatrixApi
import koma.matrix.room.naming.RoomId
import koma.network.media.parseMxc
import koma.util.onFailure
import koma.util.onSuccess
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.javafx.JavaFx
import kotlinx.coroutines.launch
import link.continuum.desktop.gui.*
import link.continuum.desktop.gui.icon.avatar.InitialIcon
import link.continuum.desktop.gui.icon.avatar.downloadImageResized
import mu.KotlinLogging
import org.controlsfx.control.Notifications

private val logger = KotlinLogging.logger {}

@ExperimentalCoroutinesApi
class DiscoveredRoomFragment(
        private val account: MatrixApi,
        private val avatarSize: Double = appState.store.settings.scaling * 32.0
): ListCell<DiscoveredRoom>(), CoroutineScope by CoroutineScope(Dispatchers.Default) {
    val root = HBox( 10.0)
    private val imageView = ImageView()
    private val initialIcon = InitialIcon(avatarSize).apply {
        this.root.removeWhen(imageView.imageProperty().isNotNull)
    }
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
        logger.debug { "discovered room $item, empty=$empty" }
        super.updateItem(item, empty)
        if (empty || item == null) {
            text = null
            graphic = null
            return
        }
        name = item.dispName()
        initialIcon.updateItem(name, hashStringColorDark(item.room_id.id))
        imageView.imageProperty().unbind()
        imageView.image = null
        item.avatar_url?.parseMxc()?.let {
            val i = downloadImageResized(it, avatarSize, account.server)
            imageView.imageProperty().bind(i)
        }


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
        stackpane {
            add(initialIcon.root)
            add(imageView)
        }
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
                    action { joinById(roomId.value, name, root, account) }
                }
                alignment = Pos.CENTER_RIGHT
            }
        }
    }
}

fun CoroutineScope.joinById(roomid: RoomId, name: String, owner: Node,
                            api: MatrixApi,
                            store: AppStore = appState.store) {
    launch {
        val rs = api.joinRoom(roomid)
        rs.onSuccess {
            launch(Dispatchers.JavaFx) { store.joinRoom(roomid, api) }
        }
        rs.onFailure {
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
