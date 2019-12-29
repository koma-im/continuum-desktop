package link.continuum.desktop.database.models

import io.requery.kotlin.asc
import io.requery.kotlin.eq
import koma.matrix.UserId
import koma.matrix.room.naming.RoomId
import link.continuum.database.models.Membership
import link.continuum.database.models.RoomPowerSettings
import link.continuum.database.models.RoomSettings
import link.continuum.database.models.defaultRoomPowerSettings
import link.continuum.desktop.Room
import link.continuum.desktop.database.RoomDataStorage
import link.continuum.desktop.util.Account
import link.continuum.desktop.util.onNull
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

fun loadRoom(dataStorage: RoomDataStorage, roomId: RoomId,
             account: Account
): Room? {
    logger.debug { "Loading room with id $roomId" }
    val data = dataStorage.data
    val settings = data.select(RoomSettings::class).where(
            RoomSettings::roomId.eq(roomId.id)
    ).get().firstOrNull() onNull {
        logger.warn { "no settings stored for room $roomId" }
    }
    val powers = data.select(RoomPowerSettings::class).where(
            RoomPowerSettings::roomId.eq(roomId.id)
    ).get().firstOrNull() onNull  {
        logger.warn { "no power settings stored for room $roomId" }
    }
    val room = Room(roomId, dataStorage,
            account = account,
            historyVisibility = settings?.historyVisibility,
            joinRule = settings?.joinRule,
            visibility = settings?.visibility,
            powerLevels = powers ?: defaultRoomPowerSettings(roomId)
    )
    val members = data.select(Membership::class).where(
            Membership::room.eq(roomId.id)
    ).orderBy(Membership::since.asc()).limit(200).get().toList()
    room.addMembers(members.map { UserId(it.person) })
    logger.debug { "loaded ${members.size} members, " +
            "there are now ${room.members.size()} members in $roomId" }
    return room
}
