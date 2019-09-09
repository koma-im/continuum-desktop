package koma.gui.view.window.chatroom.messaging.reading

import javafx.collections.ListChangeListener
import javafx.collections.ObservableList
import javafx.scene.input.ScrollEvent
import javafx.scene.layout.AnchorPane
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox
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
import kotlin.math.max

private val logger = KotlinLogging.logger {}
private val settings = appState.store.settings

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
        private val km: Koma,
        store: AppStore
) {
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

    fun scrollToBottom() {
        currentViewing?.let { roomStates.get(it) }?.following = true
        virtualList.items?.let {
            virtualList.scrollTo(it.size - 1)
        }
    }

    fun setList(msgs: ObservableList<EventItem>, roomId: RoomId) {
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
        s.latestTime = msgs.lastOrNull()?.first?.server_time ?: 0
        roomStates[roomId] = s
        scrollToBottom()
        msgs.addListener { e: ListChangeListener.Change<out EventItem> -> onAddedMessages(e, roomId) }
    }
    init {
        VBox.setVgrow(root, Priority.ALWAYS)
        HBox.setHgrow(virtualList.view, Priority.ALWAYS)
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
