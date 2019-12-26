package link.continuum.desktop.database

import io.requery.kotlin.asc
import io.requery.kotlin.desc
import io.requery.kotlin.eq
import io.requery.kotlin.ne
import javafx.scene.paint.Color
import koma.gui.element.icon.placeholder.generator.hashStringColorDark
import koma.matrix.UserId
import koma.matrix.room.naming.RoomId
import koma.network.media.MHUrl
import koma.network.media.parseMxc
import kotlinx.coroutines.flow.*
import kotlinx.serialization.internal.StringSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import kotlinx.serialization.list
import link.continuum.database.KDataStore
import link.continuum.database.models.*
import link.continuum.desktop.Room
import link.continuum.desktop.database.models.loadRoom
import link.continuum.desktop.gui.list.user.UserDataStore
import link.continuum.desktop.util.Account
import link.continuum.desktop.util.getOrNull
import link.continuum.desktop.util.toOption
import mu.KotlinLogging
import java.util.*
import java.util.concurrent.ConcurrentHashMap

private val logger = KotlinLogging.logger {}

private val json = Json(JsonConfiguration.Stable)

fun RoomId.hashColor(): Color {
    return hashStringColorDark(this.full)
}

class RoomDataStorage(
        val data: KDataStore,
        private val userDatas: UserDataStore
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
                    json.stringify(StringSerializer.list, s)
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
                    json.parse(StringSerializer.list, rec.aliases)
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
    val heroes = LatestFlowMap(save = { room: RoomId, heroes: List<UserId>, l: Long ->
        data.saveHeroes(room, heroes, l)
    }, init = { room: RoomId ->
        val records = data.select(RoomHero::class)
                .where(RoomHero::room.eq(room.id))
                .orderBy(RoomHero::since.desc())
                .limit(5).get().toList()
        val first = records.firstOrNull()
        if (first != null) {
            (first.since ?: 0L) to records.map { UserId(it.hero) }
        } else {
            val mems = data.select(Membership::class).where(
                    Membership::room.eq(room.id)
                            .and(Membership::joiningRoom.ne(false))
            ).orderBy(Membership::since.asc()).limit(5).get().map { it.person }
            logger.info { "no known heros in $room, using $mems"}
            0L to mems.map {UserId(it)}
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
                                roomDisplayName(room, heroes, userDatas)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun roomDisplayName(
        room: Room,
        heroes: LatestFlowMap<RoomId, List<UserId>>,
        userDatas: UserDataStore
): Flow<String> {
    return flow {
        emit(room.id.localstr)
        emitAll(heroes.receiveUpdates(room.id).flatMapLatest {
            logger.info { "generating room name from heros $it" }
            combine(it.map { userDatas.getNameUpdates(it) }) {
                val n = it.filterNotNull().joinToString(", ")
                logger.info { "generated name $n"}
                n
            }
        })
    }
}