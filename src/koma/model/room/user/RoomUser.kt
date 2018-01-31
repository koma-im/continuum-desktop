package model.room.user

import koma.matrix.UserId

/**
 * User states that are specific room
 * for example, whether a user is typing a message in a room
 */
class RoomUserState{
    var power: Float? = null
}

class RoomUserMap {
    val users = mutableMapOf<UserId, RoomUserState>()

    fun get(userId: UserId):RoomUserState {
        users.computeIfAbsent(userId, {RoomUserState()} )
        return users[userId]!!
    }
}
