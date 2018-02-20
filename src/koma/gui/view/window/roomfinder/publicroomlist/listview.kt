package koma.gui.view.window.roomfinder.publicroomlist

import domain.DiscoveredRoom
import javafx.beans.property.SimpleDoubleProperty
import javafx.beans.property.StringProperty
import javafx.collections.ObservableList
import javafx.geometry.Orientation
import javafx.scene.Node
import javafx.scene.control.ScrollBar
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox
import koma.gui.view.window.roomfinder.publicroomlist.listcell.DiscoveredRoomFragment
import koma.matrix.publicapi.rooms.getPublicRooms
import kotlinx.coroutines.experimental.javafx.JavaFx
import kotlinx.coroutines.experimental.launch
import rx.javafx.kt.toObservable
import rx.lang.kotlin.filterNotNull
import tornadofx.*

class PublicRoomsView(val publicRoomList: ObservableList<DiscoveredRoom>, val joinButton: Node) {

    val ui = VBox()

    lateinit var roomfield: StringProperty

    init {
        createui()
        ui.vgrow = Priority.ALWAYS
    }

    private fun createui() {
        ui.apply {
            hbox {
                label("Room")
                textfield() {
                    roomfield = textProperty()
                    textProperty().addListener({ _, _, newValue ->
                        joinButton.setDisable(newValue.trim().isEmpty())
                    })
                }
            }
            val roomlist = RoomListView(publicRoomList)
            roomlist.root.selectionModel.selectedItemProperty().toObservable()
                    .filterNotNull() // when nothing's selected
                    .subscribe {
                        roomfield.set(it.room_id)
                    }
            this+=roomlist
        }
    }
}

class RoomListView(private val roomlist: ObservableList<DiscoveredRoom>): View() {
    override val root = listview(roomlist)

    private val percent = SimpleDoubleProperty()
    private val publicRoomSrc = getPublicRooms()

    init {
        with(root) {
            vgrow = Priority.ALWAYS
            cellFragment(DiscoveredRoomFragment::class)
        }
        root.skinProperty().addListener { _o ->
            val scrollBar = findScrollBar()
            if (scrollBar != null) {
                scrollBar.valueProperty().divide(scrollBar.maxProperty())
                percent.bind(scrollBar.valueProperty())
            }
        }
        loadMoreRooms()
        percent.onChange { if (it > 0.78) loadMoreRooms() }
    }

    private fun loadMoreRooms() {
        var added = 0
        launch(JavaFx) {
            for (room in publicRoomSrc) {
                roomlist.add(room)
                added += 1
                if (added > 10) break
            }
        }
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
