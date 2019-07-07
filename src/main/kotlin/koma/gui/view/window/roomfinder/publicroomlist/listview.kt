package koma.gui.view.window.roomfinder.publicroomlist

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
import javafx.util.Callback
import koma.Koma
import koma.gui.view.window.roomfinder.publicroomlist.listcell.DiscoveredRoomFragment
import koma.gui.view.window.roomfinder.publicroomlist.listcell.joinById
import koma.koma_app.AppStore
import koma.koma_app.appState
import koma.matrix.DiscoveredRoom
import koma.matrix.publicapi.rooms.findPublicRooms
import koma.matrix.publicapi.rooms.getPublicRooms
import koma.matrix.room.naming.RoomId
import koma.matrix.room.naming.canBeValidRoomAlias
import koma.matrix.room.naming.canBeValidRoomId
import koma.util.onFailure
import koma.util.onSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.delay
import kotlinx.coroutines.javafx.JavaFx
import kotlinx.coroutines.launch
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import org.controlsfx.control.Notifications
import org.controlsfx.control.textfield.CustomTextField
import org.controlsfx.control.textfield.TextFields
import tornadofx.*
import java.util.concurrent.ConcurrentHashMap
import java.util.function.Predicate

class PublicRoomsView(publicRoomList: ObservableList<DiscoveredRoom>,
                      private val server: HttpUrl,
                      koma: Koma
) {

    val ui = VBox(5.0)

    val input: StringProperty
    private val roomlist: RoomListView

    init {
        val field = TextFields.createClearableTextField() as CustomTextField
        input = field.textProperty()
        roomlist = RoomListView(publicRoomList, input, server, koma)
        createui(field)
        ui.vgrow = Priority.ALWAYS
    }

    fun clean() = roomlist.clean()

    private fun createui(field: CustomTextField) {
        val inputStartAlias = booleanBinding(input) { value?.startsWith('#') ?: false }
        val inputStartId = booleanBinding(input) { value?.startsWith('!') ?: false }
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
                    removeWhen { inputStartAlias.not() }
                    enableWhen { inputIsAlias }
                    action { joinRoomByAlias(input.get()) }
                }
                button("Join by Room Id") {
                    removeWhen { inputStartId.not() }
                    enableWhen { inputIsId }
                    action {
                        val inputid = input.get()
                        joinById(RoomId(inputid), inputid, this, server) }
                }
            }
            this+=roomlist
        }
    }

    private fun joinRoomByAlias(alias: String) {
        val api = appState.apiClient
        api ?: return
        GlobalScope.launch {
            val rs = api.resolveRoomAlias(alias)
            rs.onSuccess {
                joinById(it.room_id, alias, this@PublicRoomsView.ui , server)
            }
            rs.onFailure {
                launch(Dispatchers.JavaFx) {
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
        input: StringProperty,
        server: HttpUrl,
        koma: Koma,
        appData: AppStore = appState.store
): View() {
    private val api = appState.apiClient!!
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
        existing.addAll(appData.joinedRoom.getIds())
        with(root) {
            vgrow = Priority.ALWAYS
            cellFactory = Callback{
                DiscoveredRoomFragment(server, koma)
            }
        }
        root.skinProperty().addListener { _ ->
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
        if (term.startsWith('#') || term.startsWith('!')) {
            return
        }
        filterTerm = term
        val words = term.split(' ')
        matchRooms.predicate = Predicate { r: DiscoveredRoom ->
            r.containsTerms(words)
        }
        GlobalScope.launch {
            delay(500)
            if (filterTerm == term) {
                curRoomSrc = getRoomSource(filterTerm)
                launch(Dispatchers.JavaFx) {
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
        GlobalScope.launch(Dispatchers.JavaFx) {
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
                RoomListingSource("", getPublicRooms(api))
            else
                RoomListingSource(it, findPublicRooms(it, api))
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
        val produce: ReceiveChannel<DiscoveredRoom>
) {
    override fun toString(): String {
        return if (term.isBlank()) "Listing for all public rooms"
        else "Listing for rooms with term $term"
    }
}
