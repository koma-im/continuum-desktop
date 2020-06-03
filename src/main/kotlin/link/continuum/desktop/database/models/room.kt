package link.continuum.desktop.database.models

import io.requery.kotlin.eq
import koma.matrix.room.naming.RoomId
import link.continuum.database.models.RoomPowerSettings
import link.continuum.database.models.RoomSettings
import link.continuum.database.models.defaultRoomPowerSettings
import link.continuum.desktop.Room
import link.continuum.desktop.database.RoomDataStorage
import link.continuum.desktop.util.Account
import link.continuum.desktop.util.onNull
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

suspend fun loadRoom(dataStorage: RoomDataStorage, roomId: RoomId,
             account: Account
): Room {
    logger.debug { "Loading room with id $roomId" }
    val data = dataStorage.data
    val settings = data.runOp {
        val c= RoomSettings::roomId.eq(roomId.id)
        select(RoomSettings::class).where(c).get().firstOrNull()
    } onNull {
        logger.warn { "no settings stored for room $roomId" }
    }
    val c = RoomPowerSettings::roomId.eq(roomId.id)
    val powers = data.runOp { select(RoomPowerSettings::class).where(c).get().firstOrNull()
    } onNull  {
        logger.warn { "no power settings stored for room $roomId" }
    }
    val room = Room(roomId, dataStorage,
            account = account,
            historyVisibility = settings?.historyVisibility,
            joinRule = settings?.joinRule,
            visibility = settings?.visibility,
            powerLevels = powers ?: defaultRoomPowerSettings(roomId)
    )
    return room
}
