package koma.gui.view.window.roomfinder.publicroomlist.listcell

import javafx.beans.property.SimpleListProperty
import koma.matrix.room.naming.RoomAlias
import org.junit.jupiter.api.assertThrows
import kotlin.test.Test

internal class DiscoveredRoomFragmentTest {
    @Test
    fun testObservableList() {
        val aliases = SimpleListProperty<RoomAlias>()
        aliases.clear()
        aliases.addAll(listOf())
        assertThrows<UnsupportedOperationException> { aliases.setAll(listOf()) }
        assertThrows<UnsupportedOperationException> { aliases.setAll(null as List<RoomAlias>?) }

    }
}
