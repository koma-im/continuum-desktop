package koma.gui.view.window.chatroom.messaging.reading

import javafx.collections.ListChangeListener
import javafx.collections.ObservableList
import javafx.scene.layout.Priority
import koma.gui.element.control.KListView
import koma.koma_app.AppStore
import koma.matrix.room.naming.RoomId
import koma.storage.message.MessageManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import link.continuum.database.models.RoomEventRow
import link.continuum.desktop.gui.HBox
import link.continuum.desktop.gui.VBox
import link.continuum.desktop.gui.message.MessageCell
import link.continuum.desktop.gui.view.AccountContext
import link.continuum.desktop.observable.MutableObservable
import mu.KotlinLogging
import kotlin.math.max

private val logger = KotlinLogging.logger {}

typealias EventItem = RoomEventRow

private class ViewRoomState(
        //timestamp of last read message
        var readStamp: Long? = null,
        var following: Boolean = true,
        var latestTime: Long = 0
)

@ExperimentalCoroutinesApi
@ObsoleteCoroutinesApi
class MessagesListScrollPane(
        context: AccountContext,
        store: AppStore
) {
    private val scope = MainScope()
    private val virtualList: KListView<EventItem, MessageCell> = KListView() {
        MessageCell(context, store)
    }.apply {
        view.styleClass.add("message-list-view")
    }
    private val listView = virtualList
    val root
        get() = virtualList.view

    val roomIdObservable = MutableObservable<RoomId>()
    private var currentViewing: RoomId? = null
    private val roomStates = mutableMapOf<RoomId, ViewRoomState>()
    fun scrollToBottom() {
        currentViewing?.let { roomStates.get(it) }?.following = true
        virtualList.items?.let {
            virtualList.scrollTo(it.size - 1)
        }
    }
    private suspend fun setList(msgs: ObservableList<EventItem>, roomId: RoomId) {
        listView.view.requestFocus()
        val cur = currentViewing
        if (cur != null) {
            val first = virtualList.visibleFirst()
            if (first != null) {
                val item = first
                logger.debug { "room $cur has been read up to ${item.server_time}" }
                roomStates[cur]?.readStamp = item.server_time
            } else {
                logger.warn { "can't find first visible message in $cur" }
            }
        }
        delay(20)
        virtualList.items = msgs
        //workaround. the first visible item may be null before any scrolling
        virtualList.flow.scrollPixels(1.0)
        currentViewing = roomId
        val s0 = roomStates[roomId]
        if (s0 != null) {
            restoreRead(s0.readStamp)
        } else {
            logger.debug { "viewing new room $roomId" }
            val s = ViewRoomState()
            s.latestTime = msgs.lastOrNull()?.server_time ?: 0
            roomStates[roomId] = s
            scrollToBottom()
            msgs.addListener { e: ListChangeListener.Change<out EventItem> -> onAddedMessages(e, roomId) }
        }
    }
    private suspend fun restoreRead(ts: Long?) {
        ts ?: run {
            logger.warn  { "read position was not stored" }
            return
        }
        logger.debug { "restoring read position $ts" }
        delay(20)
        virtualList.scrollBinarySearch(ts, { it.server_time})
        val actual = virtualList.visibleFirst()?.server_time
        if (actual!= ts) {
            logger.debug { "read position restored to $actual" }
        }
    }
    init {
        VBox.setVgrow(root, Priority.ALWAYS)
        HBox.setHgrow(virtualList.view, Priority.ALWAYS)
        var msgm: MessageManager? = null
        roomIdObservable.map { store.messages.get(it) to it
        }.flow().onEach {
            val m = it.first
            msgm = m
            if (m.shownList.isEmpty()) {
                m.loadMoreFromDatabase(null)
            }
            setList(m.shownList, it.second)
            delay(100)
        }.launchIn(scope)
        listView.flow.firstVisibleIndexObservable.flow().onEach {
            val m = msgm ?: return@onEach
            if (!(listView.items === m.shownList)) {
                logger.warn { "listView.items and current list does not match"}
                return@onEach
            }
            if (it != null && it < 1) {
                val first = m.shownList.getOrNull(it)
                m.loadMoreFromDatabase(first)
            }
        }.launchIn(scope)
    }

    private fun onAddedMessages(e : ListChangeListener.Change<out EventItem>,
                                roomId: RoomId
    ) {
        if (roomId != currentViewing) return
        val s = roomStates[roomId] ?: return
        val last = s.latestTime
        var newer = 0
        while (e.next()) {
            if (e.wasAdded()) {
                val added = e.addedSubList.map { it.server_time }
                s.latestTime = max(s.latestTime, added.max() ?: 0)
                newer += e.addedSubList.filter { it.server_time >= last }.count()
            }
        }
        if (!s.following) return
        if (newer < 4) {
            logger.debug { "got $newer new messages in $roomId, scrolling to latest" }
            scrollToBottom()
        } else {
            s.following = false
            logger.debug { "got $newer new messages in $roomId, not scrolling" }
        }
    }
}
