package koma.gui.view.chatview

import javafx.beans.binding.ObjectBinding
import javafx.beans.property.SimpleObjectProperty
import javafx.scene.control.Label
import javafx.scene.layout.BorderPane
import javafx.scene.layout.Priority
import javafx.scene.layout.StackPane
import koma.koma_app.appState
import model.Room
import tornadofx.*
import java.util.concurrent.ConcurrentHashMap

/**
 * switch between chat rooms
 */
class SwitchableRoomView(): View() {
    override val root = StackPane()

    val roomProperty = SimpleObjectProperty<Room>()
    private val viewCache = RoomViewCache()
    private val roomView: ObjectBinding<JoinedRoomView?>

    fun scroll(down: Boolean) {
        val rv = roomView.value
        rv?: return
        rv.scroll(down)
    }

    init {
        // needed for centering the placeholder
        root.hgrow = Priority.ALWAYS
        val view = BorderPane()
        view.hgrow = Priority.ALWAYS
        roomView = objectBinding(roomProperty) { value?.let { viewCache.getViewOfRoom(it) }}
        val roomNode = objectBinding(roomView) { value?.root }
        view.centerProperty().bind(roomNode)
        roomProperty.bind(appState.currRoom)
        val placeholder = Label("Select a room to start chatting")//VBox()
        root.children.addAll(
                placeholder,
                view)
    }
}

private class RoomViewCache() {
    private val cachemap = ConcurrentHashMap<Room, JoinedRoomView>()

    fun getViewOfRoom(room: Room): JoinedRoomView {
        return cachemap.computeIfAbsent(room, {JoinedRoomView(it)})
    }
}
