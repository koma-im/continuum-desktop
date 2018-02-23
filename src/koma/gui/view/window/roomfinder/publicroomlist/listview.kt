package koma.gui.view.window.roomfinder.publicroomlist

import com.github.kittinunf.result.failure
import com.github.kittinunf.result.success
import domain.DiscoveredRoom
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleDoubleProperty
import javafx.beans.property.StringProperty
import javafx.collections.ObservableList
import javafx.collections.transformation.FilteredList
import javafx.geometry.Orientation
import javafx.geometry.Pos
import javafx.scene.control.ScrollBar
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox
import koma.gui.view.window.roomfinder.publicroomlist.listcell.DiscoveredRoomFragment
import koma.gui.view.window.roomfinder.publicroomlist.listcell.joinById
import koma.matrix.publicapi.rooms.findPublicRooms
import koma.matrix.publicapi.rooms.getPublicRooms
import koma.matrix.room.naming.RoomId
import koma.matrix.room.naming.canBeValidRoomAlias
import koma.matrix.room.naming.canBeValidRoomId
import koma.util.coroutine.adapter.retrofit.awaitMatrix
import koma_app.appState
import kotlinx.coroutines.experimental.channels.ProducerJob
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.javafx.JavaFx
import kotlinx.coroutines.experimental.launch
import org.controlsfx.control.Notifications
import org.controlsfx.control.textfield.CustomTextField
import org.controlsfx.control.textfield.TextFields
import tornadofx.*
import java.util.concurrent.ConcurrentHashMap
import java.util.function.Predicate

class PublicRoomsView(val publicRoomList: ObservableList<DiscoveredRoom>) {

    val ui = VBox(5.0)

    val input: StringProperty
    private val roomlist: RoomListView

    init {
        val field = TextFields.createClearableTextField() as CustomTextField
        input = field.textProperty()
        roomlist = RoomListView(publicRoomList, input)
        createui(field)
        ui.vgrow = Priority.ALWAYS
    }

    fun clean() = roomlist.clean()

    private fun createui(field: CustomTextField) {
        val inputIsAlias = booleanBinding(input) {
            value?.let { canBeValidRoomAlias(it)} ?: false }
        val inputIsId = booleanBinding(input) {
            value?.let { canBeValidRoomId(it)  } ?: false }
        field.promptText = "#example:matrix.org"
        ui.apply {
            hbox(5.0) {
                alignment = Pos.CENTER_LEFT
                label("Filter:")
                add(field)
                button("Join by Room Alias") {
                    removeWhen { inputIsAlias.not() }
                    action { joinRoomByAlias(input.get()) }
                }
                button("Join by Room Id") {
                    removeWhen { inputIsId.not() }
                    action {
                        val inputid = input.get()
                        joinById(RoomId(inputid), inputid, this) }
                }
            }
            this+=roomlist
        }
    }

    private fun joinRoomByAlias(alias: String) {
        val api = appState.apiClient
        api ?: return
        launch {
            val rs = api.resolveRoomAlias(alias).awaitMatrix()
            rs.success {
                joinById(it.room_id, alias, this@PublicRoomsView.ui )
            }
            rs.failure {
                launch(JavaFx) {
                    Notifications.create()
                            .owner(this@PublicRoomsView.ui)
                            .title("Failed to resolve room alias $alias")
                            .position(Pos.CENTER)
                            .text(it.message)
                            .showWarning()
                }
            }
        }
    }
}

class RoomListView(
        private val roomlist: ObservableList<DiscoveredRoom>,
        private val input: StringProperty
): View() {
    private val matchRooms = FilteredList(roomlist)

    override val root = listview(matchRooms)

    // whether displayed rooms don't fill a screen
    private val enoughRooms = SimpleBooleanProperty(false)
    // scroll bar position
    private val percent = SimpleDoubleProperty()
    private val roomSources= ConcurrentHashMap<String, RoomListingSource>()

    private var filterTerm = ""
    private var curRoomSrc = getRoomSource(filterTerm)

    // rooms already joined by user or loaded in room finder
    private val existing = ConcurrentHashMap.newKeySet<RoomId>()

    init {
        appState.apiClient?.let {
            val rooms=it.profile.roomStore.roomList.map { it.id }
            existing.addAll(rooms)
        }
        with(root) {
            vgrow = Priority.ALWAYS
            cellFragment(DiscoveredRoomFragment::class)
        }
        root.skinProperty().addListener { _o ->
            val scrollBar = findScrollBar()
            if (scrollBar != null) {
                enoughRooms.bind(scrollBar.visibleProperty())
                percent.bind(scrollBar.valueProperty().divide(scrollBar.maxProperty()))
            }
        }
        loadMoreRooms(10)
        percent.onChange { if (it > 0.78) loadMoreRooms(10) }
        input.addListener { _, _, newValue -> if (newValue != null) updateFilter(newValue.trim()) }
    }

    fun clean() {
        roomSources.forEach { _, r -> r.produce.cancel() }
    }

    private fun updateFilter(term: String) {
        // all rooms unfiltered
        if (term.isBlank()) {
            matchRooms.predicate = null
            return
        }
        filterTerm = term
        val words = term.split(' ')
        matchRooms.predicate = Predicate { r: DiscoveredRoom ->
            r.containsTerms(words)
        }
        launch {
            delay(500)
            if (filterTerm == term) {
                curRoomSrc = getRoomSource(filterTerm)
                launch(JavaFx) {
                    for (room in curRoomSrc.produce) {
                        if (existing.add(room.room_id)) roomlist.add(room)
                        if (enoughRooms.get()) break
                    }
                }
            }
        }
    }

    private fun loadMoreRooms(upto: Int) {
        var added = 0
        launch(JavaFx) {
            for (room in curRoomSrc.produce) {
                if (existing.add(room.room_id)) {
                    roomlist.add(room)
                    added += 1
                }
                if (added >= upto) break
            }
        }
    }

    private fun getRoomSource(term: String): RoomListingSource {
        return roomSources.computeIfAbsent(term.trim(), {
            if (it.isBlank())
                RoomListingSource("", getPublicRooms())
            else
                RoomListingSource(it, findPublicRooms(it))
        })
    }

    private fun findScrollBar(): ScrollBar? {
        val nodes = root.lookupAll("VirtualScrollBar")
        for (node in nodes) {
            if (node is ScrollBar) {
                if (node.orientation == Orientation.VERTICAL) {
                    return node
                }
            }
        }
        System.err.println("failed to find scrollbar of listview")
        return null
    }
}

private class RoomListingSource(
        val term: String,
        val produce: ProducerJob<DiscoveredRoom>
) {
    override fun toString(): String {
        return if (term.isBlank()) "Listing for all public rooms"
        else "Listing for rooms with term $term"
    }
}
