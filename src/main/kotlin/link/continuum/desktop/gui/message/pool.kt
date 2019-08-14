package link.continuum.desktop.gui.message

import koma.gui.element.control.CellPool
import koma.gui.view.window.chatroom.messaging.reading.display.GuestAccessUpdateView
import koma.gui.view.window.chatroom.messaging.reading.display.HistoryVisibilityEventView
import koma.gui.view.window.chatroom.messaging.reading.display.room_event.m_message.MRoomMessageViewNode
import koma.gui.view.window.chatroom.messaging.reading.display.room_event.member.MRoomMemberViewNode
import koma.gui.view.window.chatroom.messaging.reading.display.room_event.room.MRoomCreationViewNode
import koma.matrix.event.room_message.MRoomMessage
import koma.matrix.event.room_message.state.MRoomCreate
import koma.matrix.event.room_message.state.MRoomGuestAccess
import koma.matrix.event.room_message.state.MRoomHistoryVisibility
import koma.matrix.event.room_message.state.MRoomMember
import link.continuum.database.models.RoomEventRow
import link.continuum.database.models.getEvent
import model.Room
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

private typealias Item = Pair<RoomEventRow, Room>

class EventCellPool: CellPool<MessageCell, Item> {
    override fun size(): Int {
        return subPools.sumBy { it.size }
    }

    override fun removeFirst(predicate: (MessageCell) -> Boolean, item: Item?): MessageCell? {
        val s = getSubPool(item)
        if (s == null){
            val ev = item?.first
            logger.trace { "no pool for ${ev}, ${ev?.getEvent()?.type}" }
            return null
        }
        val i = s.indexOfFirst(predicate)
        if (i < 0 ) return null
        return s.removeAt(i)
    }

    override fun removeLastOrNull(item: Item?): MessageCell? {
        val pile =  getSubPool(item)?: return null
        if (pile.isEmpty()) return null
        return pile.removeAt(pile.size - 1)
    }

    private fun getSubPool(item: Item?): MutableList<out MessageCell>? {
        val p = when (item?.first?.getEvent()) {
            is MRoomMember -> memberCells
            is MRoomMessage -> msgCells
            is MRoomCreate -> roomCreationCells
            is MRoomGuestAccess -> guestAccessCells
            is MRoomHistoryVisibility -> historyVisibilityCells
           // null ->
            else -> fallbackCells
        }
        return p
    }

    override fun add(cell: MessageCell) {
        if (cell.index == null) {
            logger.warn { "dropping unindexed cell $cell" }
            return
        }
        when (cell) {
            is MRoomMessageViewNode -> { msgCells.add(cell)}
            is MRoomMemberViewNode -> { memberCells.add(cell)}
            is MRoomCreationViewNode -> {roomCreationCells.add(cell)}
            is GuestAccessUpdateView -> {guestAccessCells.add(cell)}
            is HistoryVisibilityEventView -> {historyVisibilityCells.add(cell)}
            is FallbackCell -> fallbackCells.add(cell)
        }
    }

    override fun find(item: Item?, predicate: (MessageCell) -> Boolean): MessageCell? {
        return getSubPool(item)?.find(predicate)
    }

    override fun firstOrNull(item: Item?): MessageCell? {
        return getSubPool(item)?.firstOrNull()
    }

    override fun forEach(action: (MessageCell) -> Unit) {
        subPools.forEach { it.forEach(action) }
    }

    private val msgCells = mutableListOf<MRoomMessageViewNode>()
    private val memberCells = mutableListOf<MRoomMemberViewNode>()
    private val roomCreationCells = mutableListOf<MRoomCreationViewNode>()
    private val guestAccessCells = mutableListOf<GuestAccessUpdateView>()
    private val historyVisibilityCells = mutableListOf<HistoryVisibilityEventView>()
    private val fallbackCells = mutableListOf<FallbackCell>()
    private val subPools = listOf(
            msgCells,
            memberCells,
            roomCreationCells,
            guestAccessCells,
            historyVisibilityCells,
            fallbackCells)

    override fun clear() {
        subPools.forEach { it.clear() }
    }
}