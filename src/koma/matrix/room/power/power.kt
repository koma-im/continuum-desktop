package koma.matrix.room.power

import koma.matrix.UserId
import koma.matrix.event.room_message.state.RoomPowerLevelsContent
import matrix.event.room_message.RoomEventType

fun RoomPowerLevelsContent?.getUser(u: UserId): Float {
    return this?.users?.get(u) ?: 0f
}

fun RoomPowerLevelsContent?.state_default(): Float {
    return this?.state_default ?: 0f
}

fun RoomPowerLevelsContent?.getState(et: RoomEventType): Float {
    return this?.events?.get(et.toString()) ?: state_default()
}

/**
 * Can a user send a type of state event?
 */
fun RoomPowerLevelsContent?.canUserSet(user: UserId, et: RoomEventType): Boolean {
    val userLevel = getUser(user)
    val needLevel = getState(et)
    return userLevel >= needLevel
}

fun RoomPowerLevelsContent?.canUserSetStates(user: UserId): Boolean {
    val userLevel = getUser(user)
    val needLevel = state_default()
    return userLevel >= needLevel
}
