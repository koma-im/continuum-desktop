package koma.gui.view

import javafx.beans.property.SimpleObjectProperty
import javafx.collections.ObservableList
import javafx.geometry.Pos
import javafx.scene.control.Label
import javafx.scene.control.ListCell
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.scene.layout.Priority
import koma.Koma
import koma.Server
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
import koma.network.media.MHUrl
import koma.util.getOr
import kotlinx.coroutines.*
import link.continuum.database.KDataStore
import link.continuum.database.models.saveRoomAvatar
import link.continuum.database.models.saveRoomName
import link.continuum.desktop.gui.UiDispatcher
import link.continuum.desktop.gui.list.InvitationsView
import link.continuum.desktop.util.Account
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
               account: Account,
               storage: AppStore,
               scaling: Float = storage.settings.scaling
): View() {

    override val root = hbox (spacing = 5.0)

    val roomListView = RoomListView(roomList, account, storage.database)
    val invitationsView = InvitationsView(scaling = scaling.toDouble())

    val switchableRoomView = SwitchableRoomView(account.server.km, storage)

    init {
        roomListView.root.selectionModel.selectedItemProperty().onChange { room ->
            if (room != null) {
                switchableRoomView.setRoom(room)
            }
        }
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

private fun CoroutineScope.fixAvatar(room: Room) {
    if (gettingRoomAvatar.putIfAbsent(room.id, Unit) != null) return
    val name = room.displayName.get()
    logger.debug { "fixing unknown avatar of $name" }
    val api = appState.apiClient ?: return
    launch {
        val av = api.getRoomAvatar(room.id) getOr {
            logger.warn { "error fixing room $name's avatar, ${it}" }
            return@launch
        }
        withContext(UiDispatcher) {
            room.setAvatar(av)
        }
        saveRoomAvatar(appState.store.database, room.id,
                av.getOrNull()?.toString(),
                System.currentTimeMillis())
    }
}

@ExperimentalCoroutinesApi
private fun CoroutineScope.fixRoomName(room: Room) {
    if (gettingRoomName.putIfAbsent(room.id, Unit) != null) return
    val name = room.displayName.get()
    logger.debug { "fixing unknown name of $name" }
    val api = appState.apiClient ?: return
    launch {
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
class RoomFragment(private val data: KDataStore, private val koma: Koma
): ListCell<Room>(), CoroutineScope by CoroutineScope(Dispatchers.Default){
    var room: Room? = null
    private val avatar = AvatarAlways(koma)
    private val nameLabel = Label()

    private val avatarOptionalUrl = SimpleObjectProperty<Optional<MHUrl>>()
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
        avatar.bind(item.displayName, item.color, avatarUrl, item.account.server)
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
        RoomInfoDialog(room, user, data).openWindow()
    }
}


