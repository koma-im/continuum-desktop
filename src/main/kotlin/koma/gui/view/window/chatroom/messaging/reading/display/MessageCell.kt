package koma.gui.view.window.chatroom.messaging.reading.display

import javafx.geometry.Pos
import javafx.scene.control.*
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.Region
import javafx.scene.layout.VBox
import javafx.scene.text.Text
import koma.koma_app.AppStore
import koma.matrix.event.room_message.state.MRoomGuestAccess
import koma.matrix.event.room_message.state.MRoomHistoryVisibility
import koma.matrix.json.MoshiInstance
import koma.matrix.room.visibility.HistoryVisibility
import koma.util.formatJson
import kotlinx.coroutines.ExperimentalCoroutinesApi
import link.continuum.database.models.RoomEventRow
import link.continuum.database.models.getEvent
import link.continuum.desktop.gui.add
import link.continuum.desktop.gui.button
import link.continuum.desktop.gui.message.MessageCell
import link.continuum.desktop.gui.text
import link.continuum.desktop.gui.vbox
import model.Room

@ExperimentalCoroutinesApi
class HistoryVisibilityEventView(
        store: AppStore
): MessageCell(store) {
    private val sender = HBox()
    private val text = Text()
    override val center = HBox(5.0).apply {
        alignment = Pos.CENTER
        children.addAll(sender, text)
    }
    init {
        node.add(center)
    }
    override fun updateItem(item: Pair<RoomEventRow, Room>?, empty: Boolean) {
        super.updateItem(item, empty)
        if (empty || item == null) {
            graphic = null
        } else {
            val ev = item.first.getEvent()
            if (ev !is MRoomHistoryVisibility) {
                graphic = null
            } else {
                updateEvent(item.first, item.second)
                update(ev)
                graphic = node
            }
        }
    }

    fun update(ev: MRoomHistoryVisibility) {
        sender.children.clear()
        sender.children.add(senderAvatar.root)
        val t = when(ev.content.history_visibility) {
            HistoryVisibility.Invited -> "made events accessible to newly joined members " +
                    "from the point they were invited onwards"
            HistoryVisibility.Joined -> "made events accessible to newly joined members " +
                    "from the point they joined the room onwards"
            HistoryVisibility.Shared -> "made future room history visible to all members"
            HistoryVisibility.WorldReadable -> "made new events visible to the world"
        }
        text.text = t
    }
}

@ExperimentalCoroutinesApi
class GuestAccessUpdateView(store: AppStore): MessageCell(store) {
    override val center = HBox(5.0).apply {
        alignment = Pos.CENTER
    }
    init {
        node.add(center)
    }
    override fun updateItem(item: Pair<RoomEventRow, Room>?, empty: Boolean) {
        super.updateItem(item, empty)
        if (empty || item == null) {
            graphic = null
        } else {
            val ev = item.first.getEvent()
            if (ev !is MRoomGuestAccess) {
                graphic = null
            } else {
                updateEvent(item.first, item.second)
                update(ev)
                graphic = node
            }
        }
    }
    fun update(event: MRoomGuestAccess) {
        center.children.clear()
        center.children.addAll(senderAvatar.root,
                Text("set guest access to ${event.content.guest_access}")
                )
    }
}

interface ViewNode {
    val node: Region
    val menuItems: List<MenuItem>
}


class EventSourceViewer{
    private val dialog = Dialog<Unit>()
    private val textArea = TextArea()
    private var raw: String = ""
    private var processed: String = ""
    fun showAndWait(roomEvent: RoomEventRow) {
        raw = formatJson(roomEvent.json)
        processed = formatJson(MoshiInstance.roomEventAdapter.toJson(roomEvent.getEvent()))
        textArea.text = raw
        dialog.showAndWait()
    }

    init {
        textArea.isEditable = false
        HBox.setHgrow(textArea, Priority.ALWAYS)
        VBox.setVgrow(textArea, Priority.ALWAYS)
        val head = HBox().apply {
            vbox {
                text("Room Event Source")
                alignment = Pos.CENTER_LEFT
                HBox.setHgrow(this, Priority.ALWAYS)
            }
            add(ButtonBar().apply {
                button("Raw") {
                    tooltip = Tooltip("Json string from server")
                    setOnAction {
                        textArea.text = raw
                    }
                }
                button("Processed") {
                    tooltip = Tooltip("Portion of json that is supported")
                    setOnAction {
                        textArea.text = processed
                    }
                }
            })
        }
        dialog.apply {
            title = "Room Event Source"
            isResizable = true
            dialogPane.apply {
                content = VBox(5.0, head, textArea).apply {
                    VBox.setVgrow(this, Priority.ALWAYS)
                }
                buttonTypes.add(ButtonType.CLOSE)
            }
        }
    }
}
