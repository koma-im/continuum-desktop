package link.continuum.database.models

import io.requery.Column
import io.requery.Entity
import io.requery.Key
import io.requery.Persistable
import io.requery.kotlin.eq
import koma.matrix.UserId
import koma.matrix.room.naming.RoomId
import link.continuum.database.KDataStore
import mu.KotlinLogging

private val logger = KotlinLogging.logger { }

@Entity
interface RoomHero: Persistable {
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
    var hero: String

    /**
     * timestamp
     */
    var since: Long?
}

@Entity
interface Membership: Persistable {
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
     * last time the user was active in the room
     */
    var lastActive: Long?

}

fun KDataStore.saveHeroes(roomId: RoomId, heroes: List<UserId>, ts: Long) {
    val records = heroes.map {
        RoomHeroEntity().apply {
            room = roomId.full
            hero = it.full
            since = ts
        }
    }
    this.insert(records)
}

fun loadMembership(data: KDataStore, roomId: RoomId): List<Membership> {
    return data.select(Membership::class)
            .where(Membership::room.eq(roomId.id)).get().toList()
}

/**
 * rooms the given user has joined
 */
fun loadUserRooms(data: KDataStore, userId: UserId): List<RoomId> {
    return data.select(Membership::class)
            .where(Membership::person.eq(userId.str)).get().map { RoomId(it.room) }
}


fun saveUserInRoom(data: KDataStore, userId: UserId, roomId: RoomId, time: Long? = null) {
    val membership = MembershipEntity()
    membership.room = roomId.id
    membership.person = userId.str
    membership.lastActive = time
    data.upsert(membership)
}

fun removeMembership(data: KDataStore, userId: UserId, roomId: RoomId) {
    data.delete(Membership::class)
            .where(Membership::person.eq(userId.str)
                    .and(Membership::room.eq(roomId.id))).get().value()
}

