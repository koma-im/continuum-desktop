package link.continuum.desktop.database.models

import io.requery.kotlin.desc
import io.requery.kotlin.eq
import koma.koma_app.appState
import koma.matrix.room.naming.RoomId
import koma.matrix.user.identity.UserId_new
import link.continuum.database.KDataStore
import link.continuum.database.models.Membership
import link.continuum.database.models.RoomAliasRecord
import link.continuum.database.models.RoomPowerSettings
import link.continuum.database.models.RoomSettings
import model.Room
import mu.KotlinLogging
import okhttp3.HttpUrl

private val logger = KotlinLogging.logger {}

fun loadRoom(data: KDataStore, roomId: RoomId): Room? {
    logger.debug { "Loading room with id $roomId" }
    val settings = data.select(RoomSettings::class).where(
            RoomSettings::roomId.eq(roomId.id)
    ).get().firstOrNull() ?: return null

    val aliases = data.select(RoomAliasRecord::class).where(
            RoomAliasRecord::roomId.eq(roomId.id)
    ).get().toList()
    val powers = data.select(RoomPowerSettings::class).where(
            RoomPowerSettings::roomId.eq(roomId.id)
    ).get().firstOrNull()
    val room = Room(roomId, data,
            aliases = aliases,
            name = settings.roomName,
            avatar = settings.avatar?.let { HttpUrl.parse(it) },
            historyVisibility = settings.historyVisibility,
            joinRule = settings.joinRule,
            visibility = settings.visibility,
            powerLevels = powers
    )
    val members = data.select(Membership::class).where(
            Membership::room.eq(roomId.id)
    ).orderBy(Membership::lastActive.desc()).limit(200).get().toList()
    room.members.addAll(members.map {
        val u = UserId_new(it.person)
        appState.store.userStore.getOrCreateUserId(u)
    })
    return room
}
