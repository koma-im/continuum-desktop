package link.continuum.desktop.database.models

import io.requery.Column
import io.requery.Entity
import io.requery.Index
import io.requery.Persistable
import io.requery.kotlin.eq
import koma.matrix.room.naming.RoomId
import link.continuum.desktop.database.KDataStore

@Entity
interface Membership: Persistable {
    /**
     * id of room
     */
    @get:Index("room_person_membership")
    @get:Column(length = Int.MAX_VALUE, unique = true)
    var room: String

    /**
     * user id
     */
    @get:Index("room_person_membership")
    @get:Column(length = Int.MAX_VALUE, unique = true)
    var person: String

    /**
     * last time the user was active in the room
     */
    var lastActive: Long?

}

fun loadMembership(data: KDataStore, roomId: RoomId): List<Membership> {
    return data.select(Membership::class)
            .where(Membership::room.eq(roomId.id)).get().toList()
}

