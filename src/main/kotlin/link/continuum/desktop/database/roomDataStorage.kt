package link.continuum.desktop.database

import io.requery.kotlin.asc
import io.requery.kotlin.desc
import io.requery.kotlin.eq
import io.requery.kotlin.isNull
import javafx.scene.paint.Color
import koma.gui.element.icon.placeholder.generator.hashStringColorDark
import koma.koma_app.AppData
import koma.matrix.UserId
import koma.matrix.room.naming.RoomId
import koma.network.media.MHUrl
import koma.network.media.parseMxc
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.serialization.builtins.list
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
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
        val appData: AppData,
        private val userDatas: UserDataStore
) {
    private val store = ConcurrentHashMap<RoomId, Deferred<Room>>()

    suspend fun getOrCreate(roomId: RoomId, account: Account): Room {
        return coroutineScope {
            store.computeIfAbsent(roomId) {
                async {
                    loadRoom(this@RoomDataStorage, roomId, account)
                }
            }.await()
        }
    }
    val latestName = LatestFlowMap<RoomId, Optional<String>>(
            save = { roomId: RoomId, s: Optional<String>, l: Long ->
                data.runOp {
                    saveRoomName(this, roomId, s.getOrNull(), l)
                }
            },
            init = {
                data.runOp { getLatestRoomName(this, it)} ?: run {
                    0L to Optional.empty<String>()
                }
            })
    val latestCanonAlias = LatestFlowMap(
            save = { roomId: RoomId, s: Optional<String>, l: Long ->
                val rec: RoomCanonicalAlias = RoomCanonicalAliasEntity()
                rec.roomId = roomId.full
                rec.alias = s.getOrNull()
                rec.since = l
                data.runOp { upsert(rec) }
            },
            init = {
                data.runOp {
                    val c = RoomCanonicalAlias::roomId.eq(it.full)
                    select(RoomCanonicalAlias::class).where(c)
                        .get().firstOrNull() ?.let {
                            it.since to it.alias.toOption()
                        }
                }?: 0L to Optional.empty<String>()
            })
    val latestAliasList = LatestFlowMap(
            save = { roomId: RoomId, s: List<String>, l: Long ->
                val ser = try {
                    json.stringify(String.serializer().list, s)
                } catch (e: Exception) {
                    return@LatestFlowMap
                }
                val rec = RoomAliasListEntity()
                rec.roomId = roomId.full
                rec.aliases = ser
                rec.since = l
                data.runOp { upsert(rec) }
            },
            init = {roomId ->
                val c = RoomAliasList::roomId.eq(roomId.full)
                val rec = data.runOp {select(RoomAliasList::class)
                        .where(c)
                        .get().firstOrNull() } ?: return@LatestFlowMap  0L to listOf()
                val aliases = try {
                    json.parse(String.serializer().list, rec.aliases)
                } catch (e: Exception) {
                    return@LatestFlowMap 0L to listOf()
                }
                rec.since to aliases
            })
    val latestAvatarUrl = LatestFlowMap(
            save = { RoomId: RoomId, url: Optional<MHUrl>, l: Long ->
                data.runOp {
                    saveRoomAvatar(this, RoomId, url.getOrNull()?.toString(), l)
                }
            },
            init = { id ->
                val rec = data.runOp {getLatestAvatar(this, id) }
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
        data.runOp { saveHeroes(room, heroes, l) }
    }, init = { room: RoomId ->
        val c = RoomHero::room.eq(room.id)
        val records = data.runOp { select(RoomHero::class)
                .where(c)
                .orderBy(RoomHero::since.desc())
                .limit(5).get().toList()
        }
        val first = records.firstOrNull()
        if (first != null) {
            (first.since ?: 0L) to records.map { UserId(it.hero) }
        } else {
            val mems = data.runOp {
                val c1 = Membership::joiningRoom.isNull()
                val c2 = Membership::joiningRoom.eq(true)
                select(Membership::class).where(
                        Membership::room.eq(room.id).and(c2.or(c1))
                ).orderBy(Membership::since.asc()).limit(5).get().map { it.person }
            }
            logger.info { "no known heros in $room, using $mems"}
            0L to mems.map {UserId(it)}
        }
    })
    fun latestDisplayName(id: RoomId): Flow<String> {
        return latestName.receiveUpdates(id).flatMapLatest {
            val name = it?.getOrNull()
            if (name != null) {
                flowOf(name)
            } else {
                latestCanonAlias.receiveUpdates(id).flatMapLatest {
                    val canon = it?.getOrNull()
                    if (canon != null) {
                        flowOf(canon)
                    } else {
                        latestAliasList.receiveUpdates(id).flatMapLatest {
                            val first = it?.firstOrNull()
                            if (first != null) {
                                flowOf(first)
                            } else {
                                roomDisplayName(id, heroes, userDatas)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun roomDisplayName(
        room: RoomId,
        heroes: LatestFlowMap<RoomId, List<UserId>>,
        userDatas: UserDataStore
): Flow<String> {
    return flow {
        emit(room.localstr)
        emitAll(heroes.receiveUpdates(room).filterNotNull().flatMapLatest {
            logger.info { "generating room name from heros $it" }
            combine(it.map { userDatas.getNameUpdates(it) }) {
                val n = it.filterNotNull().joinToString(", ")
                logger.info { "generated name $n"}
                n
            }
        })
    }
}