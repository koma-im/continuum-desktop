package koma.model.room

import koma.matrix.room.naming.RoomAlias
import koma.storage.rooms.RoomStore

fun add_aliases_to_room(roomId: String, aliases: List<RoomAlias>) {

    val existingRoomState = RoomStore.getOrCreate(roomId)
    existingRoomState.aliases.addAll(aliases)

}
