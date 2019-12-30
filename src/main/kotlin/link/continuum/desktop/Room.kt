package link.continuum.desktop

import koma.matrix.MatrixApi
import koma.matrix.event.room_message.state.RoomPowerLevelsContent
import koma.matrix.room.naming.RoomId
import koma.matrix.room.participation.RoomJoinRules
import koma.matrix.room.visibility.HistoryVisibility
import koma.matrix.room.visibility.RoomVisibility
import koma.storage.message.MessageManager
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.flow.first
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
    @ObsoleteCoroutinesApi
    val messageManager by lazy { MessageManager(this, dataStorage.data ) }

    // whether it's listed in the public directory
    var visibility: RoomVisibility = RoomVisibility.Private
    var joinRule: RoomJoinRules = RoomJoinRules.Invite
    var histVisibility = HistoryVisibility.Shared
    private val server = account.server
    init {
        historyVisibility?.let { histVisibility = it }
        joinRule?.let { this.joinRule = it }
        visibility?.let { this.visibility = visibility }
    }
    suspend fun displayName(): String {
        return dataStorage.latestDisplayName(this).first()
    }

    fun updatePowerLevels(roomPowerLevel: RoomPowerLevelsContent) {
        powerLevels.usersDefault = roomPowerLevel.users_default
        powerLevels.stateDefault = roomPowerLevel.state_default
        powerLevels.eventsDefault = roomPowerLevel.events_default
        powerLevels.ban = roomPowerLevel.ban
        powerLevels.invite = roomPowerLevel.invite
        powerLevels.kick = roomPowerLevel.kick
        powerLevels.redact = roomPowerLevel.redact
        val data = this.dataStorage.data
        savePowerSettings(data, powerLevels)
        saveEventPowerLevels(data, id, roomPowerLevel.events)
        saveUserPowerLevels(data, id, roomPowerLevel.users)
    }

    override fun toString(): String {
        return "Room(id=$id)"
    }
}


