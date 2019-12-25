package link.continuum.desktop.database

import io.requery.kotlin.eq
import javafx.scene.paint.Color
import koma.gui.element.icon.placeholder.generator.hashStringColorDark
import koma.matrix.room.naming.RoomId
import koma.network.media.MHUrl
import koma.network.media.parseMxc
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.serialization.internal.StringSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.list
import link.continuum.database.KDataStore
import link.continuum.database.models.*
import link.continuum.desktop.Room
import link.continuum.desktop.database.models.loadRoom
import link.continuum.desktop.util.Account
import link.continuum.desktop.util.getOrNull
import link.continuum.desktop.util.toOption
import mu.KotlinLogging
import java.util.*
import java.util.concurrent.ConcurrentHashMap

private val logger = KotlinLogging.logger {}

fun RoomId.hashColor(): Color {
    return hashStringColorDark(this.full)
}

class RoomDataStorage(
        val data: KDataStore
) {
    private val store = ConcurrentHashMap<RoomId, Room>()

    fun getOrCreate(roomId: RoomId, account: Account): Room {
        val newRoom = store.computeIfAbsent(roomId) {
            loadRoom(this, roomId, account) ?: run {
                logger.info { "Room $roomId not in database" }
                Room(roomId, this, account)
            }}
        return newRoom
    }
    val latestName = LatestFlowMap<RoomId, Optional<String>>(
            save = { roomId: RoomId, s: Optional<String>, l: Long ->
                saveRoomName(data, roomId, s.getOrNull(), l)
            },
            init = {
                getLatestRoomName(data, it) ?: run {
                    0L to Optional.empty<String>()
                }
            })
    val latestCanonAlias = LatestFlowMap(
            save = { roomId: RoomId, s: Optional<String>, l: Long ->
                val rec: RoomCanonicalAlias = RoomCanonicalAliasEntity()
                rec.roomId = roomId.full
                rec.alias = s.getOrNull()
                rec.since = l
                data.upsert(rec)
            },
            init = {
                data.select(RoomCanonicalAlias::class).where(RoomCanonicalAlias::roomId.eq(it.full))
                        .get().firstOrNull() ?.let {
                            it.since to it.alias.toOption()
                        } ?:
                0L to Optional.empty<String>()
            })
    val latestAliasList = LatestFlowMap(
            save = { roomId: RoomId, s: List<String>, l: Long ->
                val ser = try {
                    Json.plain.stringify(StringSerializer.list, s)
                } catch (e: Exception) {
                    return@LatestFlowMap
                }
                val rec = RoomAliasListEntity()
                rec.roomId = roomId.full
                rec.aliases = ser
                rec.since = l
                data.upsert(rec)
            },
            init = {roomId ->
                val rec = data.select(RoomAliasList::class)
                        .where(RoomAliasList::roomId.eq(roomId.full))
                        .get().firstOrNull() ?: return@LatestFlowMap  0L to listOf()
                val aliases = try {
                    Json.plain.parse(StringSerializer.list, rec.aliases)
                } catch (e: Exception) {
                    return@LatestFlowMap 0L to listOf()
                }
                rec.since to aliases
            })
    val latestAvatarUrl = LatestFlowMap(
            save = { RoomId: RoomId, url: Optional<MHUrl>, l: Long ->
                saveRoomAvatar(data, RoomId, url.getOrNull()?.toString(), l)
            },
            init = { id ->
                val rec = getLatestAvatar(data, id)
                if (rec != null) {
                    if (rec.second.isEmpty) {
                        return@LatestFlowMap rec.first to Optional.empty<MHUrl>()
                    }
                    val s = rec.second.get()
                    val mxc = s.parseMxc()
                    if (mxc == null) {
                        logger.warn { "invalid url $s"}
                        return@LatestFlowMap rec.first to Optional.empty<MHUrl>()
                    }
                    rec.first to Optional.of(mxc)
                } else {
                    0L to Optional.empty()
                }
            })
    fun latestDisplayName(room: Room): Flow<String> {
        val id = room.id
        return latestName.receiveUpdates(id).flatMapLatest {
            val name = it.getOrNull()
            if (name != null) {
                flowOf(name)
            } else {
                latestCanonAlias.receiveUpdates(id).flatMapLatest {
                    val canon = it.getOrNull()
                    if (canon != null) {
                        flowOf(canon)
                    } else {
                        latestAliasList.receiveUpdates(id).flatMapLatest {
                            val first = it.firstOrNull()
                            if (first != null) {
                                flowOf(first)
                            } else {
                                roomDisplayName(room)
                            }
                        }
                    }
                }
            }
        }
    }
}

fun roomDisplayName(room: Room): Flow<String> {
    return flowOf(room.id.localstr)
}