package koma.storage.message

import javafx.beans.property.Property
import koma.gui.element.control.NullableIndexRange
import koma.matrix.event.room_message.RoomEvent
import koma.matrix.room.Timeline
import koma.storage.message.piece.Segment
import kotlinx.coroutines.CompletableDeferred

sealed class MessageManagerMsg

class AppendSync(val timeline: Timeline<RoomEvent>): MessageManagerMsg()

/**
 * Load some messages to show when the app starts
 */
object ShowLatest: MessageManagerMsg()

/**
 * Index of the first and last messages shown on screen
 * In order to load more messages when necessary
 */
class VisibleRange(val property: Property<NullableIndexRange>): MessageManagerMsg()

class StartFetchEarlier(val index: Long): MessageManagerMsg()

class PrependFetched(
        val key: Long,
        val messages: List<RoomEvent>,
        val newEdge: CompletableDeferred<Segment?>): MessageManagerMsg()

class FillUi(val key: Long): MessageManagerMsg()
