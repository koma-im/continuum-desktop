package koma.matrix.json

import koma.matrix.UserId
import koma.matrix.room.naming.RoomId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test

internal class NewTypeStringTest {
    @Test
    fun equals() {
        val useridstr1 = "@example:matrix.org"
        assertEquals(UserId(useridstr1), UserId(useridstr1))
        assertEquals(UserId(useridstr1).hashCode(), UserId(useridstr1).hashCode())
        assertNotEquals(UserId(useridstr1), RoomId(useridstr1))

        // not the case
        // assertNotEquals(UserId(useridstr1).hashCode(), RoomId(useridstr1).hashCode())
    }
}
