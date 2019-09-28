package koma.gui.view.chatview

import javafx.scene.control.Label
import javafx.scene.layout.*
import koma.koma_app.AppStore
import model.Room
import okhttp3.OkHttpClient

/**
 * switch between chat rooms
 */
class SwitchableRoomView(
        km: OkHttpClient,
        appStore: AppStore
) {
    val root = StackPane()

    private val roomView = JoinedRoomView(km, appStore)
    private val view = BorderPane()
    private var selected = false

    fun setRoom(room: Room) {
        roomView.setRoom(room)
        if (!selected) {
            selected= true
            root.children.addAll(view)
        }
    }

    init {
        // needed for centering the placeholder
        HBox.setHgrow(root, Priority.ALWAYS)
        VBox.setVgrow(view, Priority.ALWAYS)
        view.center = roomView.root
        val placeholder = Label("Select a room to start chatting")//VBox()
        root.children.addAll(
                placeholder)
    }
}
