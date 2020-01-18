package koma.gui.view.window.roomfinder.publicroomlist

import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.collections.transformation.FilteredList
import javafx.geometry.Orientation
import javafx.geometry.Pos
import javafx.scene.control.Button
import javafx.scene.control.ListView
import javafx.scene.control.ScrollBar
import javafx.scene.input.MouseEvent
import javafx.scene.layout.Priority
import javafx.util.Callback
import koma.gui.view.window.roomfinder.publicroomlist.listcell.DiscoveredRoomFragment
import koma.gui.view.window.roomfinder.publicroomlist.listcell.joinById
import koma.koma_app.AppStore
import koma.matrix.DiscoveredRoom
import koma.matrix.RoomListing
import koma.matrix.room.naming.RoomId
import koma.util.getOrThrow
import koma.util.onFailure
import koma.util.onSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.javafx.JavaFx
import kotlinx.coroutines.launch
import link.continuum.desktop.gui.*
import link.continuum.desktop.observable.MutableObservable
import link.continuum.desktop.observable.Observable
import link.continuum.desktop.util.Account
import mu.KotlinLogging
import org.controlsfx.control.Notifications
import org.controlsfx.control.textfield.CustomTextField
import org.controlsfx.control.textfield.TextFields
import java.util.function.Predicate

private val logger = KotlinLogging.logger {}

fun canBeValidRoomAlias(input: String): Boolean {
    val ss = input.split(':')
    if (ss.size != 2) return false
    return ss[0].startsWith('#') && ss[1].isNotEmpty()
}

fun canBeValidRoomId(input: String): Boolean {
    val ss = input.split(':')
    if (ss.size != 2) return false
    return ss[0].startsWith('!') && ss[1].isNotEmpty()
}

class PublicRoomsView(private val account: Account,
                      private val appData: AppStore
) {
    private val scope = MainScope()
    val ui = VBox(5.0)

    val input: Observable<String> = MutableObservable<String>("")
    private val roomListView: RoomListView

    init {
        val field = TextFields.createClearableTextField() as CustomTextField
        field.textProperty().addListener { _, _, newValue ->
            input as MutableObservable<String>
            input.set(newValue)
        }
        roomListView = RoomListView(input, account, appData)
        VBox.setVgrow(ui, Priority.ALWAYS)

        field.promptText = "#example:matrix.org"
        val joinByAliasButton = Button("Join by Room Alias").apply {
            showIf(false)
            setOnAction { joinRoomByAlias(input.get()) }
        }
        val joinByIdButton = Button("Join by Room ID").apply {
            showIf(false)
            setOnAction {
                val inputid = input.get()
                scope.joinById(RoomId(inputid), inputid, this, account, appData)
            }
        }
        input.flow().onEach {
            val inputStartAlias = it.startsWith('#')
            joinByAliasButton.showIf(inputStartAlias)
            val inputIsAlias = canBeValidRoomAlias(it)
            joinByAliasButton.isDisable = !inputIsAlias

            joinByIdButton.showIf(it.startsWith('!'))
            joinByIdButton.isDisable = !canBeValidRoomId(it)
        }.launchIn(scope)
        ui.apply {
            hbox(5.0) {
                alignment = Pos.CENTER_LEFT
                label("Filter:")
                add(field)
                add(joinByAliasButton)
                add(joinByIdButton)
            }
            add(roomListView.root)
        }
    }

    private fun joinRoomByAlias(alias: String) {
        val api = account.server
        scope.launch {
            val rs = api.resolveRoomAlias(alias)
            rs.onSuccess {
                joinById(it.room_id, alias, this@PublicRoomsView.ui , account, appData)
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
        input: Observable<String>,
        private val account: Account,
        appData: AppStore
) {
    private val scope = MainScope()
    private val roomlist: ObservableList<DiscoveredRoom> = FXCollections.observableArrayList<DiscoveredRoom>()
    private val matchRooms = FilteredList(roomlist)

    val root = ListView(matchRooms)
    private var scrollBar: ScrollBar? = null

    private val needLoadMore = Channel<Unit>(Channel.CONFLATED)
    // rooms already joined by user or loaded in room finder
    private val existing = hashSetOf<RoomId>()

    init {
        val myRooms = appData.keyValueStore.roomsOf(account.userId)
        existing.addAll(myRooms.joinedRoomList)
        with(root) {
            VBox.setVgrow(this, Priority.ALWAYS)
            cellFactory = Callback{
                DiscoveredRoomFragment(account, appData)
            }
        }
        root.skinProperty().addListener { _ ->
            scrollBar = findScrollBar()
            scrollBar?.let {
                it.visibleProperty().addListener { _, _, viewFilled ->
                    if (!viewFilled) needLoadMore.offer(Unit) // listView not fully filled
                }
                it.valueProperty().divide(it.maxProperty()).addListener { _, _, scrollPerc ->
                    if (scrollPerc.toFloat() > 0.78) needLoadMore.offer(Unit)
                }
            }
        }
        needLoadMore.offer(Unit)

        input.flow()
                .onEach {
                    logger.trace { "input: $it." }
                    updateFilter(it.trim())
                }.debounce(200).distinctUntilChanged().flatMapLatest {
                    logger.trace { "fetch: $it." }
                    account.publicRoomFlow(it.takeIf { it.isNotBlank() }, limit = 20).takeWhile { kResult ->
                        kResult.isSuccess
                    }.map { it.getOrThrow() }
                }.buffer(0) // slow down requests unless needed
                .zip(needLoadMore.consumeAsFlow(), ::first)
                .onEach {
                    addNewRooms(it.chunk)
                    loadMoreIfNotFilled()
                }.launchIn(scope)
    }
    //workaround some problem with zip
    private suspend fun<T> first(r: RoomListing, i: T): RoomListing {
        logger.trace { "loaded ${r.chunk.size} rooms"}
        return r
    }
    private fun loadMoreIfNotFilled() {
        val sb = scrollBar ?: run {
            logger.warn { "scrollBar is null"}
            needLoadMore.offer(Unit)
            return
        }
        if (!sb.isVisible) {
            logger.debug { "scrollBar isVisible is false"}
            needLoadMore.offer(Unit)
            return
        }
        if (sb.value / sb.max.coerceAtLeast(0.01) > 0.85) {
            logger.debug { "scrollBar is at ${sb.value} of ${sb.max}"}
            needLoadMore.offer(Unit)
            return
        }
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
        val words = term.split(' ')
        matchRooms.predicate = Predicate { r: DiscoveredRoom ->
            r.containsTerms(words)
        }
    }

    /**
     * deduplicate
     */
    private fun addNewRooms(rooms: List<DiscoveredRoom>) {
        val new = rooms.filter { existing.add(it.room_id) }
        logger.debug { "adding ${new.size} of ${rooms.size}" }
        roomlist.addAll(new)
    }

    private fun findScrollBar(): ScrollBar? {
        val nodes = root.lookupAll("VirtualScrollBar")
        for (node in nodes) {
            if (node is ScrollBar) {
                if (node.orientation == Orientation.VERTICAL) {
                    logger.debug { "found scrollbar $node"}
                    return node
                }
            }
        }
        System.err.println("failed to find scrollbar of listview")
        return null
    }
}