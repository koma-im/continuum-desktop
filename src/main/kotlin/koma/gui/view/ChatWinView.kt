package koma.gui.view

import javafx.collections.ObservableList
import javafx.geometry.Pos
import javafx.scene.control.*
import javafx.scene.paint.Color
import koma.controller.requests.membership.dialogInviteMember
import koma.controller.requests.membership.forgetRoom
import koma.controller.requests.membership.leaveRoom
import koma.gui.view.listview.RoomListView
import koma.gui.view.window.chatroom.messaging.ChatRecvSendView
import koma.gui.view.window.chatroom.roominfo.RoomInfoDialog
import koma.koma_app.AppData
import koma.koma_app.appState.apiClient
import koma.matrix.room.naming.RoomId
import koma.network.media.parseMxc
import koma.util.testFailure
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.flow.*
import link.continuum.desktop.Room
import link.continuum.desktop.database.hashColor
import link.continuum.desktop.gui.*
import link.continuum.desktop.gui.icon.avatar.Avatar2L
import link.continuum.desktop.gui.list.InvitationsView
import link.continuum.desktop.gui.view.AccountContext
import link.continuum.desktop.gui.view.RightColumn
import link.continuum.desktop.observable.MutableObservable
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
class ChatView(roomList: ObservableList<RoomId>,
               private val context: AccountContext,
               storage: Deferred<AppData>
) {
    private val scope = MainScope()
    val root = SplitPane ()

    val roomListView = RoomListView(roomList, context, storage)
    val invitationsView = InvitationsView(scaling = 1.0)

    private val roomActor = scope.actor<RoomId>(capacity = Channel.CONFLATED) { // <--- Changed here
        var initSelected = false
        val appData = storage.await()
        val messagingView = ChatRecvSendView(context, appData)
        val rightColumnDeferred = scope.async(start = CoroutineStart.LAZY) { RightColumn(context, appData, root) }
        for (room in channel) {
            messagingView.setRoom(room)
            val rightColumn = rightColumnDeferred.await()
            rightColumn.setRoom(room, context.account)
            if (!initSelected) {
                initSelected= true
                root.items.add(rightColumn.root)
                root.items.set(1, messagingView.root)
                root.setDividerPosition(1, .9)
            }
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
                roomActor.offer(room)
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
class RoomFragment(private val deferredAppData: Deferred<AppData>,
                   private val context: AccountContext
): ListCell<RoomId>() {
    private val scope = MainScope()
    private val roomObservable = MutableObservable<RoomId>()
    private val avatar = Avatar2L()
    private val nameLabel = Label()

    private val root = HBox(10.0).apply {
        minWidth = 1.0
        prefWidth = 1.0
        alignment = Pos.CENTER_LEFT
        contextMenu = ContextMenu().apply {
            item("Room Info").action {
                scope.launch { openInfoView() }
            }
            item("Invite Member"){
                action {
                    val room = roomObservable.value
                    room?.let {
                        dialogInviteMember(it)
                    } ?: logger.warn { "No room selected" }
                }
            }
            items.add(SeparatorMenuItem())
            item("Leave").action {
                val room = roomObservable.value
                room ?.let {
                    scope.launch(Dispatchers.Default) {
                        val appData = deferredAppData.await()
                        leaveRoom(it, appData)
                    }
                }
            }
            item("Forget").action {
                val room = roomObservable.value
                room ?.let {
                    scope.launch {
                        val appData = deferredAppData.await()
                        forgetRoom(context.account, it, appData)
                    }
                }
            }

        }
        add(avatar.root)
        add(nameLabel)
    }
    private val startObserving = scope.async(start = CoroutineStart.LAZY) {
        logger.trace { "start observing room" }
        val data = deferredAppData.await().roomStore
        val selected = MutableStateFlow<Boolean>(false)
        selectedProperty().addListener { _, _, newValue -> selected.value = newValue }
        val roomColor = roomObservable.flow().map { it?.hashColor()  }
        roomColor.filterNotNull().onEach {
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
        val textColor = selected.combine(roomColor) { sel, c ->
            if (sel) Color.WHITE else c
        }
        textColor.onEach {
            nameLabel.textFill = it
        }.launchIn(scope)

        roomObservable.flow()
                .onEach {
                    avatar.updateUrl(null, context.account.server)
                }
                .flatMapLatest {
                    data.latestAvatarUrl.receiveUpdates(it)
                }.onEach {
                    avatar.updateUrl(it?.getOrNull(), context.account.server)
                }.launchIn(scope)
    }

    override fun updateItem(item: RoomId?, empty: Boolean) {
        super.updateItem(item, empty)
        if (empty || item == null) {
            graphic = null
            return
        }
        if (!startObserving.isActive) {
            nameLabel.text = "room"
            startObserving.start()
        }
        roomObservable.set(item)
        graphic = root
    }

    private suspend fun openInfoView() {
        val room = item ?: return
        val user = apiClient?.userId ?: return
        val data = deferredAppData.await()
        RoomInfoDialog(data.roomStore, context, room, user).openWindow(owner = JFX.primaryStage)
    }
}


