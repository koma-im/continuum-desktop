package koma.gui.view.chatview

import javafx.beans.binding.ObjectBinding
import javafx.beans.property.SimpleObjectProperty
import javafx.scene.control.Label
import javafx.scene.layout.BorderPane
import javafx.scene.layout.Priority
import javafx.scene.layout.StackPane
import koma.Koma
import koma.Server
import koma.koma_app.appState
import link.continuum.desktop.gui.list.user.UserDataStore
import model.Room
import okhttp3.HttpUrl
import tornadofx.*
import java.util.concurrent.ConcurrentHashMap

/**
 * switch between chat rooms
 */
class SwitchableRoomView(
         km: Koma,
        userDataStore: UserDataStore
): View() {
    override val root = StackPane()

    private val roomView = JoinedRoomView(km, userDataStore)
    private val view = BorderPane()
    private var selected = false

    fun scroll(down: Boolean) {
        val rv = roomView
        rv.scroll(down)
    }
    fun setRoom(room: Room) {
        roomView.setRoom(room)
        if (!selected) {
            selected= true
            root.children.addAll(view)
        }
    }

    init {
        // needed for centering the placeholder
        root.hgrow = Priority.ALWAYS
        view.hgrow = Priority.ALWAYS
        view.center = roomView.root
        val placeholder = Label("Select a room to start chatting")//VBox()
        root.children.addAll(
                placeholder)
    }
}
