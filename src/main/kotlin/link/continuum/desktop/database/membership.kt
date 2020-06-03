package link.continuum.desktop.database

import io.requery.kotlin.asc
import io.requery.kotlin.eq
import koma.koma_app.AppData
import koma.matrix.UserId
import koma.matrix.room.naming.RoomId
import kotlinx.coroutines.*
import link.continuum.database.models.Membership
import link.continuum.database.models.newMembership
import link.continuum.desktop.gui.list.DedupList
import link.continuum.desktop.util.whenDebugging
import mu.KotlinLogging
import java.util.concurrent.ConcurrentHashMap

private val logger = KotlinLogging.logger {}

class RoomMemberships(private val data: KDataStore) {
    private val memberships = ConcurrentHashMap<RoomId, Deferred<DedupList<UserId, UserId>>>()
    suspend fun get(roomId: RoomId): DedupList<UserId, UserId> {
        return coroutineScope {
            memberships.computeIfAbsent(roomId) {
                async {
                    val membersList = DedupList<UserId, UserId> {it}
                    val c1 = Membership::room.eq(roomId.id)
                    val members = data.runOp { select(Membership::class).where(c1)
                            .orderBy(Membership::since.asc())
                            .limit(200).get().map { UserId(it.person) } }
                    logger.debug { "loaded ${members.size} members in $roomId" }
                    membersList.addAll(members)
                    membersList
                }
            }.await()
        }
    }
}
/**
 * holds changes temporarily for batch processing
 */
class MembershipChanges(
        private val appData: AppData,
        private val owner: UserId
) {
    private val data: KDataStore = appData.database
    private val joins = hashMapOf<UserId, HashMap<RoomId, Long>>()
    private val leaves = hashMapOf<UserId, HashMap<RoomId, Long>>()
    fun addJoin(UserId: UserId, RoomId: RoomId, timestamp: Long) {
        val userJoins = joins.computeIfAbsent(UserId) { hashMapOf() }
        val recentJoin = userJoins[RoomId]
        if (recentJoin != null && recentJoin > timestamp) return
        val userLeaves = leaves.computeIfAbsent(UserId) { hashMapOf()}
        val recentLeave = userLeaves[RoomId]
        if (recentLeave != null ) {
            if (recentLeave > timestamp) {
                return
            } else {
                userLeaves.remove(RoomId)
            }
        }

        userJoins[RoomId] = timestamp
    }
    fun addLeave(UserId: UserId, RoomId: RoomId, timestamp: Long) {
        val userJoins = joins.computeIfAbsent(UserId) { hashMapOf() }
        val recentJoin = userJoins[RoomId]
        if (recentJoin != null) {
            if (recentJoin > timestamp) {
                return
            } else {
                userJoins.remove(RoomId)
            }
        }
        val userLeaves = leaves.computeIfAbsent(UserId) { hashMapOf()}
        val recentLeave = userLeaves[RoomId]
        if (recentLeave != null && recentLeave > timestamp) {
            return
        }

        userLeaves[RoomId] = timestamp
    }
    /**
     * save to database and update UI data
     */
    suspend fun saveData() {
        whenDebugging{
            joins.forEach { j ->
                val u = j.key
                val uj = j.value.keys
                val uL = leaves[u]?.keys?:return@forEach
                val conflicts = uj.intersect(uL)
                check(conflicts.isEmpty()) { "user $u joins $uj leaves $uL, conflicts: $conflicts" }
            }
        }
        val roomToJoins = hashMapOf<RoomId, HashSet<UserId>>()
        joins.forEach { (userId, map) ->
            map.forEach { (roomId, _) ->
                val rJ = roomToJoins.computeIfAbsent(roomId) { hashSetOf() }
                rJ.add(userId)
            }
        }
        val roomToLeaves = hashMapOf<RoomId, HashSet<UserId>>()
        leaves.forEach { (userId, map) ->
            map.forEach { (roomId, _) ->
                val rL = roomToLeaves.computeIfAbsent(roomId) { hashSetOf() }
                rL.add(userId)
            }
        }
        withContext(Dispatchers.Main) {
            roomToLeaves.forEach {
                val ml = appData.roomMemberships.get(it.key)
                ml.removeAllById(it.value)
            }
            roomToJoins.forEach {
                val ml = appData.roomMemberships.get(it.key)
                ml.addAll(it.value)
            }
        }
        val s = sequence {
            joins.forEach { (userId, map) ->
                map.forEach { (roomId, ts) ->
                    val membership = newMembership(roomId = roomId.full, userId = userId.full, timestamp = ts, isJoin = true)
                    yield(membership)
                }
            }
            leaves.forEach { (userId, map) ->
                map.forEach { (roomId, ts) ->
                    val membership = newMembership(roomId = roomId.full, userId = userId.full, timestamp = ts, isJoin = false)
                    yield(membership)
                }
            }
        }
        data.runOp { upsert(s.asIterable()) }
    }
}