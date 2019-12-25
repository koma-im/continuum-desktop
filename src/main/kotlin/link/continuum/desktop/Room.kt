package link.continuum.desktop

import javafx.application.Platform
import koma.Server
import koma.matrix.MatrixApi
import koma.matrix.UserId
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
import link.continuum.desktop.gui.list.DedupList

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
    val members = DedupList<Pair<UserId, Server>, UserId>({it.first})

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

    fun makeUserJoined(us: UserId) {
        check(Platform.isFxApplicationThread())
        members.add(us to server)
    }

    fun removeMember(mid: UserId) {
        check(Platform.isFxApplicationThread())
        members.remove(mid to server)
    }
    fun addMembers(ms: List<UserId>) {
        members.addAll(ms.map { it to server })
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


