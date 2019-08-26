package link.continuum.desktop.database.models

import io.requery.kotlin.desc
import io.requery.kotlin.eq
import koma.matrix.UserId
import koma.matrix.room.naming.RoomId
import koma.network.media.parseMxc
import link.continuum.database.KDataStore
import link.continuum.database.models.*
import link.continuum.desktop.util.Account
import link.continuum.desktop.util.onNull
import model.Room
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

fun loadRoom(data: KDataStore, roomId: RoomId,
             account: Account
): Room? {
    logger.debug { "Loading room with id $roomId" }
    val settings = data.select(RoomSettings::class).where(
            RoomSettings::roomId.eq(roomId.id)
    ).get().firstOrNull() onNull {
        logger.warn { "no settings stored for room $roomId" }
    }

    val aliases = data.select(RoomAliasRecord::class).where(
            RoomAliasRecord::roomId.eq(roomId.id)
    ).get().toList()
    val powers = data.select(RoomPowerSettings::class).where(
            RoomPowerSettings::roomId.eq(roomId.id)
    ).get().firstOrNull() onNull  {
        logger.warn { "no power settings stored for room $roomId" }
    }
    val room = Room(roomId, data,
            aliases = aliases,
            account = account,
            historyVisibility = settings?.historyVisibility,
            joinRule = settings?.joinRule,
            visibility = settings?.visibility,
            powerLevels = powers ?: defaultRoomPowerSettings(roomId)
    )
    room.initName(getLatestRoomName(data, roomId))
    room.initAvatar(getLatestAvatar(data, roomId)?.map { it.parseMxc() })
    val members = data.select(Membership::class).where(
            Membership::room.eq(roomId.id)
    ).orderBy(Membership::lastActive.desc()).limit(200).get().toList()
    room.addMembers(members.map { UserId(it.person) })
    logger.debug { "loaded ${members.size} members, " +
            "there are now ${room.members.size()} members in $roomId" }
    return room
}
