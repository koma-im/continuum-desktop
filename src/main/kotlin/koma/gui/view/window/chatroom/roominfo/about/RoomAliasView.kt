package koma.gui.view.window.chatroom.roominfo.about

import de.jensd.fx.glyphs.materialicons.MaterialIcon
import de.jensd.fx.glyphs.materialicons.utils.MaterialIconFactory
import javafx.beans.binding.Bindings
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.geometry.Pos
import javafx.scene.control.ListCell
import javafx.scene.control.ListView
import javafx.scene.control.TextField
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.text.FontWeight
import javafx.util.Callback
import koma.gui.element.emoji.keyboard.NoSelectionModel
import koma.gui.view.window.chatroom.roominfo.about.requests.requestAddRoomAlias
import koma.gui.view.window.chatroom.roominfo.about.requests.requestSetRoomCanonicalAlias
import koma.koma_app.appState
import koma.matrix.UserId
import koma.matrix.event.room_message.RoomEventType
import koma.matrix.room.naming.RoomAlias
import koma.util.onFailure
import koma.util.onSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.javafx.JavaFx
import kotlinx.coroutines.launch
import link.continuum.database.KDataStore
import link.continuum.database.models.getChangeStateAllowed
import link.continuum.desktop.gui.JFX
import model.Room
import org.controlsfx.control.Notifications
import tornadofx.*
import java.util.concurrent.Callable

class RoomAliasForm(room: Room, user: UserId,
                    data: KDataStore
                    ): Fragment() {
    override val root: Fieldset

    init {
        val canEditCanonAlias = getChangeStateAllowed(data, room.id, user, RoomEventType.CanonAlias.toString())
        val canEdit = getChangeStateAllowed(data, room.id, user)

        this.title = "Update Aliases of Room ${room.displayName.value}"

        root = fieldset("Room Aliases") {
            vbox(5) {
                listview(room.aliases.list) {
                    prefHeight = 200.0
                    selectionModel = NoSelectionModel()
                    cellFactory = object : Callback<ListView<RoomAlias>, ListCell<RoomAlias>> {
                        override fun call(param: ListView<RoomAlias>?): ListCell<RoomAlias> {
                            return RoomAliasCell(room, canEditCanonAlias, canEdit)
                        }
                    }
                    vgrow = Priority.ALWAYS
                }
                hbox(5.0) {
                    removeWhen(SimpleBooleanProperty(canEdit).not())
                    val field = TextField()
                    field.promptText = "additional-alias"
                    val servername = room.id.servername
                    hbox {
                        alignment = Pos.CENTER
                        label("#")
                        add(field)
                        label(":")
                        label(servername)
                    }
                    val getAlias = { "#${field.text}:$servername" }
                    button("Add") { action { requestAddRoomAlias(room, getAlias()) } }
                }
            }
        }
    }
}

private fun deleteRoomAlias(room: Room, alias: RoomAlias?) {
    alias?:return
    val api = appState.apiClient
    api ?: return
    GlobalScope.launch {
        val result = api.deleteRoomAlias(alias.str)
        result.onFailure {
            val message = it.message
            launch(Dispatchers.JavaFx) {
                Notifications.create()
                        .title("Failed to delete room alias $alias")
                        .text("In room ${room.displayName.get()}\n$message")
                        .owner(JFX.primaryStage)
                        .showWarning()
            }
        }.onSuccess {
            launch(Dispatchers.JavaFx) {
                room.aliases.remove(alias)
            }
        }
    }
}

class RoomAliasCell(
        private val room: Room,
        private val canonEditAllowed: Boolean,
        private val editAllowedDef: Boolean): ListCell<RoomAlias>() {

    private val roomAlias = SimpleObjectProperty<RoomAlias>()
    private val cell = HBox(5.0)

    init {
        val text = stringBinding(roomAlias) { value?.str }
        val isCanon = Bindings.createBooleanBinding(Callable{roomAlias.value == room.canonicalAlias.value},
                roomAlias, room.canonicalAlias)
        val star = MaterialIconFactory.get().createIcon(MaterialIcon.STAR)
        val notstar = MaterialIconFactory.get().createIcon(MaterialIcon.STAR_BORDER)
        val deleteIcon = MaterialIconFactory.get().createIcon(MaterialIcon.DELETE)
        with(cell) {
            prefWidth = 1.0
            minWidth = 1.0
            alignment = Pos.CENTER_LEFT
            stackpane {
                hyperlink(graphic = notstar) {
                    tooltip("Set as Canonical Alias")
                    visibleWhen { cell.hoverProperty().and(canonEditAllowed) }
                    action { requestSetRoomCanonicalAlias(room, roomAlias.value) }
                }
                hyperlink(graphic = star) {
                    tooltip("Current Canonical Alias")
                    removeWhen { isCanon.not() }
                }
            }
            label {
                textProperty().bind(text)
                style {
                    fontWeight = FontWeight.EXTRA_BOLD
                }
            }
            lazyContextmenu {
                item("Delete") {
                    removeWhen(SimpleBooleanProperty(editAllowedDef).not())
                    action { deleteRoomAlias(room, roomAlias.value) }
                }
                item("Set Canonical") {
                    removeWhen(SimpleBooleanProperty(canonEditAllowed).not())
                    action { requestSetRoomCanonicalAlias(room, roomAlias.value) }
                }
            }
            hyperlink(graphic = deleteIcon) {
                visibleWhen { cell.hoverProperty().and(editAllowedDef) }
                action { deleteRoomAlias(room, roomAlias.value) }
            }
        }
    }

    override fun updateItem(item: RoomAlias?, empty: Boolean) {
        super.updateItem(item, empty)
        roomAlias.set(item)
        if (item != null) graphic = cell else graphic = null
    }
}
