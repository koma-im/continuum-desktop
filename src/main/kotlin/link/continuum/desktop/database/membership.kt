package link.continuum.desktop.database

import io.requery.kotlin.`in`
import io.requery.kotlin.eq
import koma.koma_app.AppData
import koma.matrix.UserId
import koma.matrix.room.naming.RoomId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import link.continuum.database.KDataStore
import link.continuum.database.models.Membership
import link.continuum.database.models.newMembership
import link.continuum.desktop.util.Account
import mu.KotlinLogging
import kotlin.math.max

private val logger = KotlinLogging.logger {}

/**
 * temporary class used to process a batch of changes
 */
class MembershipChanges(
        private val appData: AppData,
        private val owner: UserId
) {
    private val data: KDataStore = appData.database
    private val joins = hashMapOf<UserId, HashMap<RoomId, Long>>()
    private val leaves = hashMapOf<UserId, HashMap<RoomId, Long>>()
    private val ownerJoins = hashMapOf<RoomId, Long?>()
    private val ownerLeaves = hashMapOf<RoomId, Long?>()
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
    fun ownerJoins(roomIds: List<RoomId>) {
        val eventLeaves = leaves[owner]
        if (eventLeaves != null) {
            roomIds.forEach {
                if (eventLeaves.contains(it)) {
                    logger.info { "$owner is still in room $it, overriding state events" }
                    eventLeaves.remove(it)
                }
            }
        }
        val eventJoins = joins[owner]
        if (eventJoins != null) {
            ownerJoins.putAll(eventJoins)
        }
        roomIds.forEach {
            if (!ownerJoins.contains(it)) {
                logger.debug { "adding owner joined room $it"}
                ownerJoins[it] = null
            }
        }
    }
    suspend fun ownerLeaves(roomIds: Collection<RoomId>) {
        val eventJoins = joins[owner]
        if (eventJoins != null) {
            roomIds.forEach {
                if (eventJoins.containsKey(it)) {
                    logger.info { "$owner has left room $it, overriding state events" }
                    eventJoins.remove(it)
                }
            }
        }
        val eventLeaves = leaves[owner]
        if (eventLeaves != null) {
            ownerLeaves.putAll(eventLeaves)
        }
        roomIds.forEach {
            if (!ownerLeaves.containsKey(it)) {
                logger.debug { "adding owner $owner left room $it"}
                ownerLeaves[it] = null
            }
        }
    }

    /**
     * save to database and update UI data
     */
    suspend fun saveData(account: Account) {
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
            appData.joinedRoom.removeAllById(ownerLeaves.keys)
            ownerJoins.forEach {
                appData.joinRoom(it.key, account)
            }
            roomToLeaves.forEach {
                val room = appData.roomStore.getOrCreate(it.key, account)
                room.members.removeAllById(it.value)
            }
            roomToJoins.forEach {
                val room = appData.roomStore.getOrCreate(it.key, account)
                room.members.addAll(it.value.map { it to account.server })
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
        data.upsert(s.asIterable())
        val ownerRooms = ownerJoins.keys.plus(ownerLeaves.keys).map {it.full}
        val records = data.select(Membership::class)
                .where(Membership::room.eq(owner.full)
                        .and(Membership::room.`in`(ownerRooms)))
                .get().groupBy {
                    val recordTs = it.since ?: return@groupBy true
                    val roomId = RoomId(it.room)
                    recordTs <= (ownerJoins[roomId]?:0) || recordTs <= (ownerLeaves[roomId]?:0)
                }
        val notUpdates = records[false]
        if (notUpdates != null) {
            logger.debug { "Membership in db is newer: $notUpdates" }
        }
        val ownerJoinMut = ownerJoins.toMutableMap()
        val ownerLeaveMut = ownerLeaves.toMutableMap()
        val ownerUpdates = records[true]
        if (ownerUpdates != null) {
            val newTimes = ownerUpdates.map {
                val roomId = RoomId(it.room)
                val j = if (ownerJoinMut.containsKey(roomId)) {
                    ownerJoinMut
                } else { ownerLeaveMut}.remove(roomId)!!
                it.since = max(it.since ?: 0, j ?: 0)
                it
            }
            data.upsert(newTimes)
        }
        val newMemberships =ownerJoinMut.asSequence().map {
            newMembership(roomId = it.key.full, userId = owner.full, timestamp = it.value, isJoin = true)
        }.plus(ownerLeaveMut.asSequence().map {
            newMembership(roomId = it.key.full, userId = owner.full, timestamp = it.value, isJoin = false)
        })
        data.upsert(newMemberships.asIterable())
    }
}