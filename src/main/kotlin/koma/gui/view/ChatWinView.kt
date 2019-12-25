package koma.gui.view

import javafx.application.Platform
import javafx.collections.ObservableList
import javafx.geometry.Pos
import javafx.scene.control.*
import javafx.scene.paint.Color
import koma.controller.requests.membership.dialogInviteMember
import koma.controller.requests.membership.leaveRoom
import koma.gui.view.listview.RoomListView
import koma.gui.view.window.chatroom.messaging.ChatRecvSendView
import koma.gui.view.window.chatroom.roominfo.RoomInfoDialog
import koma.koma_app.AppStore
import koma.koma_app.appState.apiClient
import koma.matrix.room.naming.RoomId
import koma.network.media.parseMxc
import koma.util.testFailure
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import link.continuum.desktop.Room
import link.continuum.desktop.database.RoomDataStorage
import link.continuum.desktop.database.hashColor
import link.continuum.desktop.gui.*
import link.continuum.desktop.gui.icon.avatar.Avatar2L
import link.continuum.desktop.gui.list.InvitationsView
import link.continuum.desktop.gui.view.RightColumn
import link.continuum.desktop.observable.MutableObservable
import link.continuum.desktop.util.Account
import link.continuum.desktop.util.getOrNull
import mu.KotlinLogging
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
) {
    val root = SplitPane ()

    val roomListView = RoomListView(roomList, account, storage.roomStore)
    val invitationsView = InvitationsView(scaling = scaling.toDouble())


    val messagingView by lazy { ChatRecvSendView(account.server.okHttpClient, storage) }
    val rightColumn by lazy { RightColumn(account, storage, root) }

    private var initSelected = false

    private fun setRoom(room: Room) {
        check(Platform.isFxApplicationThread())
        messagingView.setRoom(room)
        rightColumn.setRoom(room)
        if (!initSelected) {
            initSelected= true
            root.items.add(rightColumn.root)
            root.items.set(1, messagingView.root)
            root.setDividerPosition(1, .9)
        }
    }
    init {
        root.items.add(VBox().apply {
            add(invitationsView.list)
            add(roomListView.root)
        })
        root.setDividerPosition(0, .2)
        val placeholder = HBox().apply {
            alignment = Pos.CENTER
            vbox {
                alignment = Pos.CENTER
                label("Select a room to start chatting")
            }
        }
        root.items.add(placeholder)
        roomListView.root.selectionModel.selectedItemProperty().addListener { _, _, room ->
            if (room != null) {
                setRoom(room)
            }
        }
        if (roomList.isNotEmpty()) {
            roomListView.root.selectionModel.selectFirst()
        }
    }

}

private val gettingRoomAvatar = ConcurrentHashMap<RoomId, Unit>()
private val gettingRoomName = ConcurrentHashMap<RoomId, Unit>()

fun CoroutineScope.fixAvatar(room: Room) {
    if (gettingRoomAvatar.putIfAbsent(room.id, Unit) != null) return
    val name = room.id.localstr
    logger.debug { "fixing unknown avatar of $name" }
    val api = room.account
    val data = room.dataStorage
    launch {
        val (av, f, r) = api.getRoomAvatar(room.id)
        if(r.testFailure(av, f)) {
            logger.warn { "error fixing room $name's avatar, $f" }
            return@launch
        }
        val str = av.getOrNull()
        val time = System.currentTimeMillis()
        if (str != null) {
            str.parseMxc()?.let {
                data.latestAvatarUrl.update(room.id, Optional.of(it), time)
            }
        } else {
            data.latestAvatarUrl.update(room.id, Optional.empty(), time)
        }
    }
}

@ExperimentalCoroutinesApi
private fun CoroutineScope.fixRoomName(room: Room) {
    if (gettingRoomName.putIfAbsent(room.id, Unit) != null) return
    val name = room.id.localstr
    logger.debug { "fixing unknown name of $name" }
    val api = room.account
    val data = room.dataStorage
    launch {
        val (n, f, r) = api.getRoomName(room.id)
        if (r.testFailure(n, f)){
            logger.warn { "error fixing room $name's name, ${f}" }
            return@launch
        }
        val time = System.currentTimeMillis()
        data.latestName.update(room.id, n, time)
    }
}

@ExperimentalCoroutinesApi
class RoomFragment(private val data: RoomDataStorage
): ListCell<Room>() {
    private val scope = MainScope()
    private val roomObservable = MutableObservable<Room>()
    private val avatar = Avatar2L()
    private val nameLabel = Label()
    override fun updateItem(item: Room?, empty: Boolean) {
        super.updateItem(item, empty)
        if (empty || item == null) {
            graphic = null
            return
        }
        roomObservable.set(item)
        graphic = root
    }

    private val root = HBox(10.0).apply {
        minWidth = 1.0
        prefWidth = 1.0
        alignment = Pos.CENTER_LEFT
        contextMenu = ContextMenu().apply {
            item("Room Info").action { openInfoView() }
            item("Invite Member"){
                action {
                    val room = roomObservable.getOrNull()
                    room?.let {
                        dialogInviteMember(it.id)
                    } ?: logger.warn { "No room selected" }
                }
            }
            items.add(SeparatorMenuItem())
            item("Leave").action {
                val room = roomObservable.getOrNull()
                room ?.let { leaveRoom(it) }
            }

        }
        add(avatar.root)
        add(nameLabel)
    }

    init {
        val selected = MutableObservable<Boolean>(false)
        selectedProperty().addListener { _, _, newValue -> selected.set(newValue) }
        val roomColor = roomObservable.map { it.id.hashColor() }
        roomColor.flow().onEach {
            avatar.initialIcon.updateColor(it)
        }.launchIn(scope)

        roomObservable.flow()
                .onEach {
                    avatar.initialIcon.updateString("")
                    nameLabel.text = ""
                }
                .flatMapLatest {
                    data.latestDisplayName(it)
                }.onEach {
                    nameLabel.text = it
                    avatar.initialIcon.updateString(it)
                }
                .launchIn(scope)

        // reverse text brightness when selected
        val textColor = selected.flow().combine(roomColor.flow()) { sel, c ->
            if (sel) Color.WHITE else c
        }
        textColor.onEach {
            nameLabel.textFill = it
        }.launchIn(scope)

        roomObservable.flow()
                .onEach {
                    avatar.updateUrl(null, it.account.server)
                }
                .flatMapLatest {
                    data.latestAvatarUrl.receiveUpdates(it.id)
                }.onEach {
                    avatar.updateUrl(it.getOrNull(), roomObservable.get().account.server)
                }.launchIn(scope)
    }

    private fun openInfoView() {
        val room = item ?: return
        val user = apiClient?.userId ?: return
        RoomInfoDialog(room, user).openWindow(owner = JFX.primaryStage)
    }
}


