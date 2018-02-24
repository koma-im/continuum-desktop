package koma.matrix.room.admin

import koma.matrix.room.naming.RoomId
import koma.matrix.room.visibility.RoomVisibility

class CreateRoomSettings(
        val room_alias_name: String,
        val visibility: RoomVisibility)

data class CreateRoomResult(
        val room_id: RoomId)
