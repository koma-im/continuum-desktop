package koma.gui.view.window.chatroom.messaging.reading

import javafx.application.Platform
import javafx.collections.ListChangeListener
import javafx.collections.ObservableList
import javafx.scene.layout.Priority
import koma.gui.element.control.KListView
import koma.koma_app.AppStore
import koma.matrix.room.naming.RoomId
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import link.continuum.database.models.RoomEventRow
import link.continuum.database.models.getEvent
import link.continuum.desktop.gui.HBox
import link.continuum.desktop.gui.VBox
import link.continuum.desktop.gui.message.EventCellPool
import link.continuum.desktop.gui.message.MessageCell
import link.continuum.desktop.gui.message.createCell
import model.Room
import mu.KotlinLogging
import okhttp3.OkHttpClient
import kotlin.math.max

private val logger = KotlinLogging.logger {}

typealias EventItem =Pair<RoomEventRow, Room>

private class ViewRoomState(
        //timestamp of last read message
        var readStamp: Long? = null,
        var following: Boolean = true,
        var latestTime: Long = 0
)

@ExperimentalCoroutinesApi
@ObsoleteCoroutinesApi
class MessagesListScrollPane(
        private val km: OkHttpClient,
        store: AppStore
): CoroutineScope by CoroutineScope(Dispatchers.Main)  {
    private val virtualList: KListView<EventItem, MessageCell> = KListView(EventCellPool()) {
        logger.trace { "creating cell for ${it?.first?.getEvent()?.type}" }
        createCell(it?.first?.getEvent(), km, store)
    }.apply {
        view.styleClass.add("message-list-view")
    }
    private val listView = virtualList
    val root
        get() = virtualList.view

    private var currentViewing: RoomId? = null
    private val roomStates = mutableMapOf<RoomId, ViewRoomState>()

    private val selectRoom = Channel<Pair<ObservableList<EventItem>, RoomId>>(Channel.CONFLATED)

    fun scrollToBottom() {
        currentViewing?.let { roomStates.get(it) }?.following = true
        virtualList.items?.let {
            virtualList.scrollTo(it.size - 1)
        }
    }

    fun setRoom(msgs: ObservableList<EventItem>, roomId: RoomId) {
        selectRoom.offer(msgs to roomId)
    }
    private suspend fun setList(msgs: ObservableList<EventItem>, roomId: RoomId) {
        listView.view.requestFocus()
        val cur = currentViewing
        if (cur != null) {
            val first = virtualList.visibleFirst()
            if (first != null) {
                val item = first
                logger.debug { "room $cur has been read up to ${item.first.server_time}" }
                roomStates[cur]?.readStamp = item.first.server_time
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
            s.latestTime = msgs.lastOrNull()?.first?.server_time ?: 0
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
        virtualList.scrollBinarySearch(ts, { it.first.server_time})
        val actual = virtualList.visibleFirst()?.first?.server_time
        if (actual!= ts) {
            logger.debug { "read position restored to $actual" }
        }
    }
    init {
        VBox.setVgrow(root, Priority.ALWAYS)
        HBox.setHgrow(virtualList.view, Priority.ALWAYS)
        launch(Dispatchers.Main) {
            for ((m, r) in selectRoom) {
                setList(m, r)
                delay(100)
            }
        }
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
                val added = e.addedSubList.map { it.first.server_time }
                s.latestTime = max(s.latestTime, added.max() ?: 0)
                newer += e.addedSubList.filter { it.first.server_time >= last }.count()
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
