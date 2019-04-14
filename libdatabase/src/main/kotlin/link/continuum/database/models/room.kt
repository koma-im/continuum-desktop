package link.continuum.database.models

import io.requery.Column
import io.requery.Entity
import io.requery.Key
import io.requery.Persistable
import io.requery.kotlin.eq
import koma.matrix.UserId
import koma.matrix.room.naming.RoomId
import koma.matrix.room.participation.RoomJoinRules
import koma.matrix.room.visibility.HistoryVisibility
import koma.matrix.room.visibility.RoomVisibility
import link.continuum.database.KDataStore
import link.continuum.libutil.`?or?`
import link.continuum.libutil.`?or`
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

@Entity
interface RoomSettings: Persistable {
    @get:Key
    var roomId: String

    var visibility: RoomVisibility?
    var joinRule: RoomJoinRules?
    var historyVisibility: HistoryVisibility?
    var roomName: String?
    var avatar: String?
}

@Entity
interface RoomPowerSettings: Persistable {
    @get:Key
    var roomId: String

    var usersDefault: Float?
    var stateDefault: Float?
    var eventsDefault: Float?
    var ban: Float?
    var invite: Float?
    var kick: Float?
    var redact: Float?
}

fun defaultRoomPowerSettings(id: RoomId): RoomPowerSettings {
    val levels: RoomPowerSettings = RoomPowerSettingsEntity()
    levels.roomId = id.id
    levels.stateDefault = 0f
    return levels
}

@Entity
interface RoomAliasRecord: Persistable  {
    @get:Key
    var roomId: String

    var alias: String
    var since: Long
    var canonical: Boolean
}

/**
 * power levels needed to modify states
 */
@Entity
interface EventPower: Persistable {
    /**
     * id of room
     */
    @get:Key
    @get:Column(length = Int.MAX_VALUE)
    var room: String

    /**
     * type of state event
     */
    @get:Key
    @get:Column(length = Int.MAX_VALUE)
    var eventType: String

    /**
     * the power level needed
     */
    @get:Column(nullable = false)
    var power: Int
}

@Entity
interface UserPower: Persistable {
    /**
     * id of room
     */
    @get:Key
    @get:Column(length = Int.MAX_VALUE)
    var room: String

    /**
     * user id
     */
    @get:Key
    @get:Column(length = Int.MAX_VALUE)
    var person: String

    /**
     * the power level a user has in a room
     */
    @get:Column(nullable = false)
    var power: Int
}

fun getRoomMemberPower(data: KDataStore, roomId: RoomId, userId: UserId): Float {
    return data.select(UserPower::class).where(UserPower::room.eq(roomId.id)
            .and(UserPower::person.eq(userId.str))).get().firstOrNull()?.power?.toFloat()`?or?` {
        logger.debug { "no power level set for $userId in $roomId, querying room default" }
        data.select(RoomPowerSettings::class).where(RoomPowerSettings::roomId.eq(roomId.id)).get().firstOrNull()?.usersDefault
    } `?or` {
        logger.debug { "no default power level set for user in $roomId, using default" }
        0f
    }
}

fun getChangeStateAllowed(data: KDataStore, roomId: RoomId, userId: UserId): Boolean {
    val u = getRoomMemberPower(data, roomId, userId)
    val req = data.select(RoomPowerSettings::class).where(RoomPowerSettings::roomId.eq(roomId.id)).get().firstOrNull()
            ?.stateDefault`?or` {
        logger.debug { "no default power level set for state events in $roomId, using default 0" }
        0f
    }
    return u >= req
}

fun getChangeStateAllowed(data: KDataStore, roomId: RoomId, userId: UserId, type: String): Boolean {
    val u = getRoomMemberPower(data, roomId, userId)
    val req = data.select(EventPower::class).where(EventPower::room.eq(roomId.id)
            .and(EventPower::eventType.eq(type))).get().firstOrNull()?.power?.toFloat() `?or?` {
        logger.debug { "no power level set for state event $type in $roomId, querying room default" }
        data.select(RoomPowerSettings::class).where(RoomPowerSettings::roomId.eq(roomId.id)).get().firstOrNull()?.stateDefault
    } `?or` {
        logger.debug { "no default power level set for state event $type in $roomId, using default" }
        0f
    }
    return u >= req
}

fun savePowerSettings(data: KDataStore, settings: RoomPowerSettings) {
    data.upsert(settings)
}

fun saveEventPowerLevels(data: KDataStore, roomId: RoomId, levels: Map<String, Float>) {
    val records: List<EventPower> = levels.map { entry ->
        val record: EventPower = EventPowerEntity()
        record.eventType = entry.key
        record.power = entry.value.toInt()
        record.room = roomId.id
        record
    }
    data.upsert(records)
}

fun saveUserPowerLevels(data: KDataStore, roomId: RoomId, levels: Map<UserId, Float>) {
    val records = levels.map { entry ->
        val record: UserPower = UserPowerEntity()
        record.person = entry.key.str
        record.power = entry.value.toInt()
        record.room = roomId.id
        record
    }
    data.upsert(records)
}
