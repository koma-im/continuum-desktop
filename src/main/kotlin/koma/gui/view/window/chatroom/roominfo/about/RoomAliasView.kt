package koma.gui.view.window.chatroom.roominfo.about

import de.jensd.fx.glyphs.materialicons.MaterialIcon
import de.jensd.fx.glyphs.materialicons.utils.MaterialIconFactory
import javafx.beans.binding.Bindings
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.geometry.Pos
import javafx.scene.control.*
import javafx.scene.layout.GridPane
import javafx.scene.layout.Priority
import javafx.scene.text.FontWeight
import javafx.stage.Stage
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
import link.continuum.desktop.gui.*
import link.continuum.desktop.gui.stringBindingNullable as stringBinding
import model.Room
import org.controlsfx.control.Notifications
import java.util.concurrent.Callable

class RoomAliasForm(room: Room, user: UserId,
                    data: KDataStore
                    ) {
    val root: VBox
    val stage = Stage()
    init {
        val canEditCanonAlias = getChangeStateAllowed(data, room.id, user, RoomEventType.CanonAlias.toString())
        val canEdit = getChangeStateAllowed(data, room.id, user)

        stage.title = "Update Aliases of Room ${room.displayName.value}"

        root = VBox(5.0).apply {
            text("Room Aliases")
            vbox() {
                spacing = 5.0
                add(ListView<RoomAlias>(room.aliases.list).apply {
                    prefHeight = 200.0
                    selectionModel = NoSelectionModel()
                    cellFactory = object : Callback<ListView<RoomAlias>, ListCell<RoomAlias>> {
                        override fun call(param: ListView<RoomAlias>?): ListCell<RoomAlias> {
                            return RoomAliasCell(room, canEditCanonAlias, canEdit)
                        }
                    }
                    VBox.setVgrow(this, Priority.ALWAYS)
                })
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
                add(Hyperlink().apply {
                    graphic = notstar
                    tooltip("Set as Canonical Alias")
                    visibleWhen(booleanBinding(cell.hoverProperty()) {canonEditAllowed && value == true})
                    setOnAction { requestSetRoomCanonicalAlias(room, roomAlias.value) }
                })
                add(Hyperlink().apply {
                graphic = star
                    tooltip("Current Canonical Alias")
                    removeWhen { isCanon.not() }
                })
            }
            label {
                textProperty().bind(text)
                style = "-fx-font-weight: bold;"
            }
            contextMenu = ContextMenu().apply {
                item("Delete") {
                    removeWhen(SimpleBooleanProperty(editAllowedDef).not())
                    action { deleteRoomAlias(room, roomAlias.value) }
                }
                item("Set Canonical") {
                    removeWhen(SimpleBooleanProperty(canonEditAllowed).not())
                    action { requestSetRoomCanonicalAlias(room, roomAlias.value) }
                }
            }
            add(Hyperlink().apply {
                graphic = deleteIcon
                visibleWhen(booleanBinding(cell.hoverProperty(), { editAllowedDef && value == true}))
                setOnAction { deleteRoomAlias(room, roomAlias.value) }
            })
        }
    }

    override fun updateItem(item: RoomAlias?, empty: Boolean) {
        super.updateItem(item, empty)
        roomAlias.set(item)
        if (item != null) graphic = cell else graphic = null
    }
}
