package koma.matrix.room.naming

data class ResolveRoomAliasResult(
        val room_id: RoomId,
        val servers: List<String>
)
