package koma.gui.view.window.roomfinder

import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox
import koma.gui.view.window.roomfinder.publicroomlist.PublicRoomsView
import koma.koma_app.appState
import koma.matrix.DiscoveredRoom
import koma.storage.persistence.settings.AppSettings
import okhttp3.HttpUrl
import tornadofx.*

private val settings: AppSettings = appState.store.settings

class RoomFinder(server: HttpUrl): Fragment() {
    override val root = VBox(5.0)

    private val publicRoomList: ObservableList<DiscoveredRoom> = FXCollections.observableArrayList<DiscoveredRoom>()
    val pubs: PublicRoomsView

    fun open() {
        val s = this.openWindow()
        s ?: return
        s.setOnHidden {
            // cleaning up
            pubs.clean()
        }
    }

    init {
        this.title = "Room Finder"
        pubs = PublicRoomsView(publicRoomList, server)
        root.apply {
            this.minWidth = 600.0
            vgrow = Priority.ALWAYS
            style {
                paddingAll = 10
                fontSize = settings.scaling.em
            }
            add(pubs.ui)
        }
    }
}
