package koma.gui.view.window.chatroom.roominfo.about

import de.jensd.fx.glyphs.materialicons.MaterialIcon
import de.jensd.fx.glyphs.materialicons.utils.MaterialIconFactory
import javafx.beans.binding.Bindings
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.geometry.Pos
import javafx.scene.control.*
import javafx.scene.layout.Priority
import javafx.stage.Stage
import javafx.util.Callback
import koma.gui.element.emoji.keyboard.NoSelectionModel
import koma.gui.view.window.chatroom.roominfo.about.requests.requestAddRoomAlias
import koma.gui.view.window.chatroom.roominfo.about.requests.requestSetRoomCanonicalAlias
import koma.koma_app.appState
import koma.matrix.UserId
import koma.matrix.event.room_message.RoomEventType
import koma.matrix.room.naming.RoomAlias
import koma.matrix.room.naming.RoomId
import koma.util.onFailure
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.javafx.JavaFx
import link.continuum.database.models.getChangeStateAllowed
import link.continuum.desktop.database.RoomDataStorage
import link.continuum.desktop.gui.*
import link.continuum.desktop.util.getOrNull
import mu.KotlinLogging
import org.controlsfx.control.Notifications
import java.util.concurrent.Callable

private val logger = KotlinLogging.logger {}
private typealias RoomAliasStr = String

class RoomAliasForm(room: RoomId, user: UserId,
                    dataStorage: RoomDataStorage
                    ) {
    private val scope = MainScope()
    val root: VBox
    val stage = Stage().apply {
        setOnHiding {
            scope.cancel()
        }
    }
    init {
        val data = dataStorage.data
        val canEditCanonAlias = SimpleBooleanProperty(false)
        val canEdit = SimpleBooleanProperty(false)
        scope.launch {
            data.runOp {
                val d = this
                canEditCanonAlias.set(getChangeStateAllowed(d, room, user, RoomEventType.CanonAlias.toString()))
                canEdit.set(getChangeStateAllowed(d, room, user))
            }
        }
        val canonAlias = SimpleStringProperty()
        dataStorage.latestCanonAlias.receiveUpdates(room).onEach {
            val n = it?.getOrNull()
            canonAlias.set(n)
            stage.title = "Update Aliases of $n"
        }.launchIn(scope)

        root = VBox(5.0).apply {
            text("Room Aliases")
            vbox() {
                spacing = 5.0
                add(ListView<RoomAliasStr>().apply {
                    val listView = this
                    prefHeight = 200.0
                    selectionModel = NoSelectionModel()
                    cellFactory = Callback<ListView<RoomAliasStr>, ListCell<RoomAliasStr>> {
                        RoomAliasCell(room,
                                canEditCanonAlias,
                                canonAlias,
                                canEdit)
                    }
                    VBox.setVgrow(this, Priority.ALWAYS)
                    scope.launch {
                        dataStorage.latestAliasList.receiveUpdates(room).collect {
                            listView.items.setAll(it)
                        }
                    }
                })
                hbox(5.0) {
                    removeWhen(canEdit.not())
                    val field = TextField()
                    field.promptText = "additional-alias"
                    val servername = room.servername
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

private fun deleteRoomAlias(room: RoomId, alias: RoomAliasStr?) {
    alias?:return
    val api = appState.apiClient
    api ?: return
    GlobalScope.launch {
        val result = api.deleteRoomAlias(alias)
        result.onFailure {
            val message = it.message
            launch(Dispatchers.JavaFx) {
                Notifications.create()
                        .title("Failed to delete room alias $alias")
                        .text("In room ${room}\n$message")
                        .owner(JFX.primaryStage)
                        .showWarning()
            }
        }
    }
}

class RoomAliasCell(
        private val room: RoomId,
        private val canonEditAllowed: SimpleBooleanProperty,
        private val canonicalAlias: SimpleStringProperty,
        private val editAllowedDef: SimpleBooleanProperty
): ListCell<RoomAliasStr>() {

    private val roomAlias = SimpleObjectProperty<RoomAliasStr>()
    private val cell = HBox(5.0)

    init {
        val text = roomAlias
        val isCanon = Bindings.createBooleanBinding(Callable {
            roomAlias.value == canonicalAlias.value
        },
                roomAlias, canonicalAlias)
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
                    visibleWhen(cell.hoverProperty().and(canonEditAllowed))
                    setOnAction {
                        roomAlias.get()?.also {
                            requestSetRoomCanonicalAlias(room, RoomAlias( it))
                        }
                    }
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
                    removeWhen(editAllowedDef.not())
                    action { deleteRoomAlias(room, roomAlias.value) }
                }
                item("Set Canonical") {
                    removeWhen(canonEditAllowed.not())
                    action {
                        roomAlias.get()?.let {
                            requestSetRoomCanonicalAlias(room, RoomAlias(it))
                        }
                    }
                }
                Unit
            }
            add(Hyperlink().apply {
                graphic = deleteIcon
                visibleWhen(cell.hoverProperty().and(editAllowedDef))
                setOnAction { deleteRoomAlias(room, roomAlias.value) }
            })
        }
    }

    override fun updateItem(item: RoomAliasStr?, empty: Boolean) {
        super.updateItem(item, empty)
        if (empty || item == null) {
            graphic = null
            return
        }
        roomAlias.set(item)
        graphic = cell
    }
}
