package koma.matrix.event.context

import koma.matrix.event.room_message.RoomEvent


data class ContextResponse(
        /**
         *A token that can be used to paginate backwards with.
         */
        val start: String,
        val end: String,
        val events_before: List<RoomEvent>,
        val event: RoomEvent,
        val events_after: List<RoomEvent>,
        val state: List<Map<String, Any>>
)
