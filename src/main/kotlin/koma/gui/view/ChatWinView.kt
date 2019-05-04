package koma.gui.view

import javafx.collections.ObservableList
import javafx.geometry.Pos
import javafx.scene.control.Label
import javafx.scene.control.ListCell
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.scene.layout.Priority
import koma.controller.requests.membership.leaveRoom
import koma.gui.element.icon.AvatarAlways
import koma.gui.view.chatview.SwitchableRoomView
import koma.gui.view.listview.RoomListView
import koma.gui.view.window.chatroom.roominfo.RoomInfoDialog
import koma.koma_app.AppStore
import koma.koma_app.appState.apiClient
import kotlinx.coroutines.ExperimentalCoroutinesApi
import link.continuum.database.KDataStore
import model.Room
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import tornadofx.*


/**
 * a list of joined rooms used for switching
 * and a view of the actual chat room showing messages and members
 * Created by developer on 2017/6/21.
 */

@ExperimentalCoroutinesApi
class ChatView(roomList: ObservableList<Room>,
               server: HttpUrl,
               data: KDataStore,
               storage: AppStore,
               httpClient: OkHttpClient
): View() {

    override val root = vbox (spacing = 5.0)

    val roomListView = RoomListView(roomList, server, data, client = httpClient)
    val switchableRoomView = SwitchableRoomView(server, storage.userData, httpClient)

    init {
        root.addEventFilter(KeyEvent.KEY_PRESSED, { e ->
            if (e.code == KeyCode.PAGE_DOWN) switchableRoomView.scroll(true)
            else if (e.code == KeyCode.PAGE_UP) switchableRoomView.scroll(false)
        })

        with(root) {
            hbox() {
                vgrow = Priority.ALWAYS

                add(roomListView)

                add(switchableRoomView)
            }

        }


    }

}

class RoomFragment(private val data: KDataStore, private val client: OkHttpClient): ListCell<Room>() {
    var room: Room? = null
    private val avatar = AvatarAlways(client = client)
    private val nameLabel = Label()

    override fun updateItem(item: Room?, empty: Boolean) {
        super.updateItem(item, empty)
        if (empty || item == null) {
            graphic = null
            return
        }
        room = item
        avatar.bind(item.displayName, item.color, item.avatar)
        nameLabel.textProperty().cleanBind(item.displayName)
        nameLabel.textFill = item.color

        graphic = root
    }

    private val root = hbox(spacing = 10.0) {
        minWidth = 1.0
        prefWidth = 1.0
        alignment = Pos.CENTER_LEFT
        contextmenu {
            item("Room Info").action { openInfoView() }
            separator()
            item("Leave").action {
                room ?.let { leaveRoom(it) }
            }

        }
        add(avatar)
        add(nameLabel)
    }

    private fun openInfoView() {
        val room = item ?: return
        val user = apiClient?.userId ?: return
        RoomInfoDialog(room, user, data, client = client).openWindow()
    }
}


