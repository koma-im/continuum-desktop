package link.continuum.database.models

import io.requery.*
import io.requery.kotlin.desc
import io.requery.kotlin.eq
import koma.matrix.UserId
import koma.matrix.room.naming.RoomId
import koma.matrix.room.participation.RoomJoinRules
import koma.matrix.room.visibility.HistoryVisibility
import koma.matrix.room.visibility.RoomVisibility
import link.continuum.database.KDataStore
import link.continuum.desktop.util.toOption
import mu.KotlinLogging
import java.util.*

private val logger = KotlinLogging.logger {}

@Entity
interface RoomSettings: Persistable {
    @get:Key
    var roomId: String

    var visibility: RoomVisibility?
    var joinRule: RoomJoinRules?
    var historyVisibility: HistoryVisibility?
}


@Entity
interface RoomName: Persistable {
    @get:Index()
    @get:Column(length = Int.MAX_VALUE, nullable = false)
    var roomId: String

    @get:Column(length = Int.MAX_VALUE)
    var roomName: String?

    @get:Column(length = Int.MAX_VALUE, nullable = false)
    var since: Long
}

@Entity
interface RoomAvatar: Persistable {
    @get:Index()
    @get:Column(length = Int.MAX_VALUE, nullable = false)
    var roomId: String

    /**
     * URL
     */
    @get:Column(length = Int.MAX_VALUE)
    var avatar: String?

    @get:Column(length = Int.MAX_VALUE, nullable = false)
    var since: Long
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
interface RoomAliasList: Persistable  {
    @get:Key
    var roomId: String

    var aliases: String
    var since: Long
}

@Entity
interface RoomCanonicalAlias: Persistable  {
    @get:Key
    var roomId: String

    var alias: String?
    var since: Long
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
            .and(UserPower::person.eq(userId.str))).get().firstOrNull()?.power?.toFloat() ?: run {
        logger.debug { "no power level set for $userId in $roomId, querying room default" }
        data.select(RoomPowerSettings::class).where(RoomPowerSettings::roomId.eq(roomId.id)).get().firstOrNull()?.usersDefault
    }?: run {
        logger.debug { "no default power level set for user in $roomId, using default" }
        0f
    }
}

fun getChangeStateAllowed(data: KDataStore, roomId: RoomId, userId: UserId): Boolean {
    val u = getRoomMemberPower(data, roomId, userId)
    val req = data.select(RoomPowerSettings::class).where(RoomPowerSettings::roomId.eq(roomId.id)).get().firstOrNull()
            ?.stateDefault ?:run {
        logger.debug { "no default power level set for state events in $roomId, using default 0" }
        0f
    }
    return u >= req
}

fun getChangeStateAllowed(data: KDataStore, roomId: RoomId, userId: UserId, type: String): Boolean {
    val u = getRoomMemberPower(data, roomId, userId)
    val req = data.select(EventPower::class).where(EventPower::room.eq(roomId.id)
            .and(EventPower::eventType.eq(type))).get().firstOrNull()?.power?.toFloat() ?: run {
        logger.debug { "no power level set for state event $type in $roomId, querying room default" }
        data.select(RoomPowerSettings::class).where(RoomPowerSettings::roomId.eq(roomId.id)).get().firstOrNull()?.stateDefault
    } ?:run {
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


fun saveRoomName(data: KDataStore, roomId: RoomId,
                 nick: String?, timestamp: Long) {
    val d = data.select(RoomName::class) where (RoomName::roomId.eq(roomId.str)
            and RoomName::since.eq(timestamp)
            )
    val e = d.get().firstOrNull()
    if (e != null && e.roomName == nick) {
        logger.trace { "already saved Name $nick of room $roomId with time $timestamp" }
        return
    }
    val t: RoomName = RoomNameEntity()
    t.roomId = roomId.str
    t.roomName = nick
    t.since = timestamp
    data.insert(t)
}

fun getLatestRoomName(data: KDataStore, roomId: RoomId): Pair<Long, Optional<String>>? {
     data.select(RoomName::class)
            .where(RoomName::roomId.eq(roomId.str))
            .orderBy(RoomName::since.desc())
            .get().use {
                 it.iterator(0, 1).use {
                     if (it.hasNext()) {
                         val rec = it.next()
                         val n = rec.roomName
                         if (n == null) {
                             logger.debug { "room $roomId has empty name in db" }
                             return rec.since to Optional.empty()
                         } else {
                             logger.debug { "room $roomId has name $n in db" }
                             return rec.since to Optional.of( n)
                         }
                     } else {
                         logger.debug { "no name recorded for room $roomId" }
                         return null
                     }
                 }
             }
}


fun saveRoomAvatar(data: KDataStore, roomId: RoomId, avatar: String?, timestamp: Long) {
    val d = data.select(RoomAvatar::class) where (
            RoomAvatar::roomId.eq(roomId.str)
            and RoomAvatar::since.eq(timestamp)
            )
    val e = d.get().firstOrNull()
    if (e != null && e.avatar == avatar) {
        logger.trace { "already saved Avatar $avatar of room $roomId with time $timestamp" }
        return
    }
    val t: RoomAvatar = RoomAvatarEntity()
    t.roomId = roomId.str
    t.avatar = avatar
    t.since = timestamp
    data.insert(t)
}

fun getLatestAvatar(data: KDataStore, roomId: RoomId): Pair<Long, Optional<String>>? {
    return data.select(RoomAvatar::class)
            .where(RoomAvatar::roomId.eq(roomId.str))
            .orderBy(RoomAvatar::since.desc())
            .get().use { it.firstOrNull() }?.let {
                it.since to it.avatar.toOption()
            }
}
