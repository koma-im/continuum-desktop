package link.continuum.database.models

import io.requery.*
import io.requery.sql.KotlinEntityDataStore
import koma.matrix.UserId
import koma.matrix.room.naming.RoomId
import mu.KotlinLogging

private val logger = KotlinLogging.logger { }

typealias KDataStore = KotlinEntityDataStore<Persistable>

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
    @get:Index()
    @get:Column(length = Int.MAX_VALUE)
    var person: String

    var joiningRoom: Boolean?

    /**
     * timestamp of last state change
     */
    var since: Long?
}

fun newMembership(roomId: String, userId: String, timestamp: Long?, isJoin: Boolean): Membership {
    val membership: Membership = MembershipEntity()
    membership.since = timestamp
    membership.joiningRoom = isJoin
    membership.room = roomId
    membership.person = userId
    return membership
}
fun KDataStore.saveHeroes(roomId: RoomId, heroes: List<UserId>, ts: Long) {
    val records = heroes.map {
        RoomHeroEntity().apply {
            room = roomId.full
            hero = it.full
            since = ts
        }
    }
    this.upsert(records)
}