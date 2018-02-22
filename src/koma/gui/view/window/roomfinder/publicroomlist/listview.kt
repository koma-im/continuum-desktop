package koma.gui.view.window.roomfinder.publicroomlist

import com.github.kittinunf.result.failure
import com.github.kittinunf.result.success
import domain.DiscoveredRoom
import javafx.beans.property.SimpleDoubleProperty
import javafx.beans.property.SimpleStringProperty
import javafx.collections.ObservableList
import javafx.geometry.Orientation
import javafx.geometry.Pos
import javafx.scene.control.ScrollBar
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox
import koma.controller.requests.membership.joinRoomById
import koma.gui.view.window.roomfinder.publicroomlist.listcell.DiscoveredRoomFragment
import koma.matrix.publicapi.rooms.getPublicRooms
import koma.matrix.room.naming.canBeValidRoomAlias
import koma.matrix.room.naming.canBeValidRoomId
import koma.util.coroutine.adapter.retrofit.awaitMatrix
import koma_app.appState
import kotlinx.coroutines.experimental.javafx.JavaFx
import kotlinx.coroutines.experimental.launch
import org.controlsfx.control.Notifications
import org.controlsfx.control.textfield.CustomTextField
import org.controlsfx.control.textfield.TextFields
import tornadofx.*

class PublicRoomsView(val publicRoomList: ObservableList<DiscoveredRoom>) {

    val ui = VBox(5.0)

    val input  = SimpleStringProperty()

    init {
        createui()
        ui.vgrow = Priority.ALWAYS
    }

    private fun createui() {
        val inputIsAlias = booleanBinding(input) {
            value?.let { canBeValidRoomAlias(it)} ?: false }
        val inputIsId = booleanBinding(input) {
            value?.let { canBeValidRoomId(it)  } ?: false }
        val field = TextFields.createClearableTextField() as CustomTextField
        field.promptText = "#example:matrix.org"
        input.bind(field.textProperty())
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
                    action { joinRoomById(input.get()) }
                }
            }
            val roomlist = RoomListView(publicRoomList)
            this+=roomlist
        }
    }

    private fun joinRoomByAlias(alias: String) {
        val api = appState.apiClient
        api ?: return
        launch {
            val rs = api.resolveRoomAlias(alias).awaitMatrix()
            rs.success {
                joinRoomById(it.room_id)
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
