package koma.matrix.event.room_message.timeline

import koma.matrix.event.parse
import koma.matrix.event.room_message.RoomMessage
import koma.matrix.sync.RawMessage
import matrix.room.Timeline

fun Timeline<RawMessage>.parse(): Timeline<RoomMessage>{
    return Timeline<RoomMessage>(
            this.events.map { it.parse() },
            this.limited,
            this.prev_batch
    )
}
