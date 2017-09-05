package koma.model.room

import koma.matrix.room.naming.RoomAlias
import koma_app.appState

fun add_aliases_to_room(roomId: String, aliases: List<RoomAlias>) {

    val existingRoomState = appState.getOrCreateRoom(roomId)
    existingRoomState.aliases.addAll(aliases)

}
