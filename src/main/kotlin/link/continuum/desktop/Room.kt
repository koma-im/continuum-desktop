package link.continuum.desktop

import koma.matrix.MatrixApi
import koma.matrix.event.room_message.state.RoomPowerLevelsContent
import koma.matrix.room.naming.RoomId
import koma.matrix.room.participation.RoomJoinRules
import koma.matrix.room.visibility.HistoryVisibility
import koma.matrix.room.visibility.RoomVisibility
import link.continuum.database.models.*
import link.continuum.desktop.database.RoomDataStorage

class Room(
        val id: RoomId,
        val dataStorage: RoomDataStorage,
        val account: MatrixApi,
        historyVisibility: HistoryVisibility? = null,
        joinRule: RoomJoinRules? = null,
        visibility: RoomVisibility? = null,
        var powerLevels: RoomPowerSettings = defaultRoomPowerSettings(id)
) {
    // whether it's listed in the public directory
    var visibility: RoomVisibility = RoomVisibility.Private
    var joinRule: RoomJoinRules = RoomJoinRules.Invite
    var histVisibility = HistoryVisibility.Shared
    init {
        historyVisibility?.let { histVisibility = it }
        joinRule?.let { this.joinRule = it }
        visibility?.let { this.visibility = visibility }
    }

    suspend fun updatePowerLevels(roomPowerLevel: RoomPowerLevelsContent) {
        powerLevels.usersDefault = roomPowerLevel.users_default
        powerLevels.stateDefault = roomPowerLevel.state_default
        powerLevels.eventsDefault = roomPowerLevel.events_default
        powerLevels.ban = roomPowerLevel.ban
        powerLevels.invite = roomPowerLevel.invite
        powerLevels.kick = roomPowerLevel.kick
        powerLevels.redact = roomPowerLevel.redact
        val data = this.dataStorage.data
        data.runOp {
            also {
                savePowerSettings(it, powerLevels)
                saveEventPowerLevels(it, id, roomPowerLevel.events)
                saveUserPowerLevels(it, id, roomPowerLevel.users)
            }
        }
    }

    override fun toString(): String {
        return "Room(id=$id)"
    }
}


