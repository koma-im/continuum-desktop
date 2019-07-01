package koma.gui.view

import javafx.beans.binding.Bindings
import javafx.beans.property.SimpleObjectProperty
import javafx.collections.ObservableList
import javafx.geometry.Pos
import javafx.scene.control.Label
import javafx.scene.control.ListCell
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.scene.layout.Priority
import koma.controller.requests.membership.dialogInviteMember
import koma.controller.requests.membership.leaveRoom
import koma.gui.element.icon.AvatarAlways
import koma.gui.view.chatview.SwitchableRoomView
import koma.gui.view.listview.RoomListView
import koma.gui.view.window.chatroom.roominfo.RoomInfoDialog
import koma.koma_app.AppStore
import koma.koma_app.appState
import koma.koma_app.appState.apiClient
import koma.matrix.room.naming.RoomId
import koma.util.getOr
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import link.continuum.database.KDataStore
import link.continuum.database.models.saveRoomAvatar
import link.continuum.database.models.saveRoomName
import link.continuum.desktop.gui.UiDispatcher
import link.continuum.desktop.gui.list.InvitationsView
import link.continuum.desktop.util.http.mapMxc
import link.continuum.libutil.getOrNull
import model.Room
import mu.KotlinLogging
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import tornadofx.*
import java.util.*
import java.util.concurrent.ConcurrentHashMap

private val logger = KotlinLogging.logger {}

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
               httpClient: OkHttpClient,
               scaling: Float = appState.store.settings.scaling
): View() {

    override val root = hbox (spacing = 5.0)

    val roomListView = RoomListView(roomList, server, data, client = httpClient)
    val invitationsView = InvitationsView(client = httpClient, scaling = scaling.toDouble())

    val switchableRoomView = SwitchableRoomView(server, storage.userData, httpClient)

    init {
        root.addEventFilter(KeyEvent.KEY_PRESSED, { e ->
            if (e.code == KeyCode.PAGE_DOWN) switchableRoomView.scroll(true)
            else if (e.code == KeyCode.PAGE_UP) switchableRoomView.scroll(false)
        })

        with(root) {
            vgrow = Priority.ALWAYS

            vbox(5.0) {
                add(invitationsView.list)
                add(roomListView)
            }

            add(switchableRoomView)
        }


    }

}

private val gettingRoomAvatar = ConcurrentHashMap<RoomId, Unit>()
private val gettingRoomName = ConcurrentHashMap<RoomId, Unit>()

private fun fixAvatar(room: Room) {
    if (gettingRoomAvatar.putIfAbsent(room.id, Unit) != null) return
    val name = room.displayName.get()
    logger.debug { "fixing unknown avatar of $name" }
    val api = appState.apiClient ?: return
    GlobalScope.launch {
        val av = api.getRoomAvatar(room.id) getOr {
            logger.warn { "error fixing room $name's avatar, ${it}" }
            return@launch
        }
        withContext(UiDispatcher) {
            room.setAvatar(av, api.server)
        }
        saveRoomAvatar(appState.store.database, room.id,
                av.map { mapMxc(it, api.server) }.getOrNull()?.toString(),
                System.currentTimeMillis())
    }
}

@ExperimentalCoroutinesApi
private fun fixRoomName(room: Room) {
    if (gettingRoomName.putIfAbsent(room.id, Unit) != null) return
    val name = room.displayName.get()
    logger.debug { "fixing unknown name of $name" }
    val api = appState.apiClient ?: return
    GlobalScope.launch {
        val n = api.getRoomName(room.id) getOr {
            logger.warn { "error fixing room $name's name, ${it}" }
            return@launch
        }
        withContext(UiDispatcher) {
            room.initName(n)
        }
        saveRoomName(appState.store.database, room.id, n.getOrNull(), System.currentTimeMillis())
    }
}

@ExperimentalCoroutinesApi
class RoomFragment(private val data: KDataStore, private val client: OkHttpClient): ListCell<Room>() {
    var room: Room? = null
    private val avatar = AvatarAlways(client = client)
    private val nameLabel = Label()

    private val avatarOptionalUrl = SimpleObjectProperty<Optional<HttpUrl>>()
    private val avatarUrl = objectBinding(avatarOptionalUrl) {value?.getOrNull()}

    override fun updateItem(item: Room?, empty: Boolean) {
        super.updateItem(item, empty)
        if (empty || item == null) {
            graphic = null
            return
        }
        room = item
        if (item.avatar.value == null) {
            fixAvatar(item)
        }
        if (item.name.value == null) {
            fixRoomName(item)
        }

        avatarOptionalUrl.cleanBind(item.avatar)
        avatar.bind(item.displayName, item.color, avatarUrl)
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
            item("Invite Member"){
                action {
                    room?.let {
                        dialogInviteMember(it.id)
                    } ?: logger.warn { "No room selected" }
                }
            }
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


