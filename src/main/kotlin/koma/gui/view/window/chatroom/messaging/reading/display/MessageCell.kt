package koma.gui.view.window.chatroom.messaging.reading.display

import javafx.geometry.Pos
import javafx.scene.control.*
import javafx.scene.layout.Priority
import javafx.scene.layout.Region
import javafx.scene.text.Text
import koma.koma_app.AppStore
import koma.matrix.event.room_message.MRoomGuestAccess
import koma.matrix.event.room_message.MRoomHistoryVisibility
import koma.matrix.room.visibility.HistoryVisibility
import koma.util.formatJson
import kotlinx.coroutines.ExperimentalCoroutinesApi
import link.continuum.database.models.RoomEventRow
import link.continuum.database.models.getEvent
import link.continuum.desktop.gui.*
import link.continuum.desktop.gui.message.MessageCellContent
import link.continuum.desktop.gui.util.Recyclable
import link.continuum.desktop.util.http.MediaServer

@ExperimentalCoroutinesApi
class HistoryVisibilityEventView(
        store: AppStore
): MessageCellContent<MRoomHistoryVisibility> {
    private val sender = HBox()
    private val text = Text()
    private val avatar = Recyclable(store.userData.avatarPool)
    override val root = HBox(5.0).apply {
        alignment = Pos.CENTER
        children.addAll(sender, text)
    }
    init {
    }
    override fun update(message: MRoomHistoryVisibility, server: MediaServer) {
        val a = avatar.get()
        a.updateUser(message.sender, server)
        sender.children.clear()
        sender.children.add(a.root)
        val t = when (message.content.history_visibility) {
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
class GuestAccessUpdateView(store: AppStore): MessageCellContent<MRoomGuestAccess> {
    override val root = HBox(5.0).apply {
        alignment = Pos.CENTER
    }
    private val avatar = Recyclable(store.userData.avatarPool)

    override fun update(message: MRoomGuestAccess, server: MediaServer) {
        val a = avatar.get()
        a.updateUser(message.sender, server)
        root.children.clear()
        root.children.addAll(a.root,
                Text("set guest access to ${message.content.guest_access}")
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
        processed = roomEvent.getEvent()?.stringifyPretty() ?: ""
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
