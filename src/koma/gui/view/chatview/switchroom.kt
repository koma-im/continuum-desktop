package koma.gui.view.chatview

import javafx.beans.property.SimpleObjectProperty
import javafx.scene.layout.Priority
import javafx.scene.layout.StackPane
import koma_app.appState
import model.Room
import tornadofx.*
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * switch between chat rooms
 */
class SwitchableRoomView(): View() {
    override val root = StackPane()

    val roomProperty = SimpleObjectProperty<Optional<Room>>()
    private val viewCache = RoomViewCache()

    init {
        root.hgrow = Priority.ALWAYS
        roomProperty.onChange {
            if (it != null && it.isPresent) {
                root.replaceChildren(viewCache.getViewOfRoom(it.get()))
            }
        }
        roomProperty.bind(appState.currRoom)
    }
}

private class RoomViewCache() {
    private val cachemap = ConcurrentHashMap<Room, JoinedRoomView>()

    fun getViewOfRoom(room: Room): JoinedRoomView {
        return cachemap.computeIfAbsent(room, {JoinedRoomView(it)})
    }
}
