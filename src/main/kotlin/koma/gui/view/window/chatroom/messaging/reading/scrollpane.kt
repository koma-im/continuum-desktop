package koma.gui.view.window.chatroom.messaging.reading

import javafx.beans.property.SimpleBooleanProperty
import javafx.collections.ListChangeListener
import javafx.collections.ObservableList
import javafx.scene.input.ScrollEvent
import javafx.scene.layout.AnchorPane
import javafx.scene.layout.Priority
import koma.Koma
import koma.gui.element.control.KListView
import koma.koma_app.AppStore
import koma.koma_app.appState
import koma.matrix.room.naming.RoomId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.ObsoleteCoroutinesApi
import link.continuum.database.models.RoomEventRow
import link.continuum.database.models.getEvent
import link.continuum.desktop.gui.message.EventCellPool
import link.continuum.desktop.gui.message.MessageCell
import link.continuum.desktop.gui.message.createCell
import model.Room
import mu.KotlinLogging
import tornadofx.*

private val logger = KotlinLogging.logger {}
private val settings = appState.store.settings

typealias EventItem =Pair<RoomEventRow, Room>

private class ViewRoomState(
        // whether the user has caught up to the newest messages
        //whether the list should scroll automatically
        val following:SimpleBooleanProperty = SimpleBooleanProperty(true),
        //timestamp of last read message
        var readStamp: Long? = null,
        // timestamp of lastest message in the room
        var lastTime: Long = 0
)

@ExperimentalCoroutinesApi
@ObsoleteCoroutinesApi
class MessagesListScrollPane(
        private val km: Koma,
        store: AppStore
) {
    private val virtualList: KListView<EventItem, MessageCell> = KListView(EventCellPool()) {
        logger.trace { "creating cell for ${it?.first?.getEvent()?.type}" }
        createCell(it?.first?.getEvent(), km, store)
    }
    val root
        get() = virtualList.view

    private var currentViewing: RoomId? = null
    // decide whether to scroll
    private val followingLatest = SimpleBooleanProperty(true)
    private val roomStates = mutableMapOf<RoomId, ViewRoomState>()

    fun scrollToBottom() {
        followingLatest.set(true)
        virtualList.items?.let {
            virtualList.scrollTo(it.size - 1)
        }
    }

    fun setList(msgs: ObservableList<EventItem>, roomId: RoomId) {
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
        virtualList.items = msgs
        //workaround. the first visible item may be null before any scrolling
        virtualList.flow.scrollPixels(1.0)
        currentViewing = roomId
        val s0 = roomStates[roomId]
        if (s0 != null) {
            s0.readStamp?.let {
                logger.debug { "restore read position $it" }
                virtualList.scrollBinarySearch(it, { it.first.server_time})
                val actual = virtualList.visibleFirst()?.first?.server_time
                if (actual!= it) {
                    logger.debug { "read position restored to $actual" }
                }
            } ?: logger.warn  { "read position in $roomId is not stored" }
            return
        }
        logger.debug { "viewing new room $roomId" }
        val s = ViewRoomState()
        roomStates[roomId] = s
        s.lastTime = Math.max(s.lastTime, msgs.lastOrNull()?.first?.server_time?: 0)
        msgs.addListener { e: ListChangeListener.Change<out EventItem> -> onAddedMessages(e, roomId) }
    }
    init {
        root.vgrow = Priority.ALWAYS

        virtualList.view.hgrow = Priority.ALWAYS

        scrollToBottom()
        virtualList.addEventFilter(ScrollEvent.SCROLL) { _: ScrollEvent ->
            onScroll()
        }
    }

    private fun onAddedMessages(e : ListChangeListener.Change<out EventItem>,
                                roomId: RoomId
    ) {
        val s = roomStates[roomId]
        if (s == null) return
        if (followingLatest.get()) {
            if (e.next() && e.wasAdded()) {
                val added = e.addedSubList
                val lastAdd = added.lastOrNull()?.first?.getEvent()?.origin_server_ts
                if (lastAdd != null && lastAdd > s.lastTime - 5000) {
                    //roughly latest
                    s.lastTime = lastAdd
                    if (added.size > 9) {
                        //added too much
                        followingLatest.set(false)
                    } else if (currentViewing != roomId) {
                        //new message in a not selected room
                        followingLatest.set(false)
                    } else {
                        scrollToBottom()
                    }
                }
            }
        }
    }

    /**
     * remove the scrollToBottom button when the visible area goes downward
     */
    private fun onScroll() {
        val visibleLastIndex = virtualList.visibleIndexRange.value?.endInclusive
        visibleLastIndex ?:return
        val msgList = virtualList.items ?: return
        val loadedLastIndex = msgList.size - 1
        if (visibleLastIndex >= loadedLastIndex - 2) {
            followingLatest.set(true)
        } else {
            followingLatest.set(false)
        }
    }
}
