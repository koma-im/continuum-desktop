package koma.gui.view.chatview

import javafx.beans.binding.ObjectBinding
import javafx.beans.property.SimpleObjectProperty
import javafx.scene.layout.BorderPane
import javafx.scene.layout.Priority
import koma_app.appState
import model.Room
import tornadofx.*
import java.util.concurrent.ConcurrentHashMap

/**
 * switch between chat rooms
 */
class SwitchableRoomView(): View() {
    override val root = BorderPane()

    val roomProperty = SimpleObjectProperty<Room>()
    private val viewCache = RoomViewCache()
    private val roomView: ObjectBinding<JoinedRoomView?>

    fun scroll(down: Boolean) {
        val rv = roomView.value
        rv?: return
        rv.scroll(down)
    }

    init {
        root.hgrow = Priority.ALWAYS
        roomView = objectBinding(roomProperty) { value?.let { viewCache.getViewOfRoom(it) }}
        val roomNode = objectBinding(roomView) { value?.root }
        root.centerProperty().bind(roomNode)
        roomProperty.bind(appState.currRoom)
        with(root) {
            bottom {
                label("Select a room to start chatting") {
                    removeWhen { roomNode.isNotNull }
                }
            }
        }
    }
}

private class RoomViewCache() {
    private val cachemap = ConcurrentHashMap<Room, JoinedRoomView>()

    fun getViewOfRoom(room: Room): JoinedRoomView {
        return cachemap.computeIfAbsent(room, {JoinedRoomView(it)})
    }
}
