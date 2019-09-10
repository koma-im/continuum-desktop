package koma.gui.view.window.roomfinder

import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.geometry.Insets
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox
import koma.gui.view.window.roomfinder.publicroomlist.PublicRoomsView
import koma.koma_app.appState
import koma.matrix.DiscoveredRoom
import koma.storage.persistence.settings.AppSettings
import link.continuum.desktop.gui.JFX
import link.continuum.desktop.gui.add
import link.continuum.desktop.util.Account
import tornadofx.Fragment

private val settings: AppSettings = appState.store.settings

class RoomFinder(
        account: Account
): Fragment() {
    override val root = VBox(5.0)

    private val publicRoomList: ObservableList<DiscoveredRoom> = FXCollections.observableArrayList<DiscoveredRoom>()
    val pubs: PublicRoomsView

    fun open() {
        val s = this.openWindow(owner = JFX.primaryStage)
        s ?: return
        s.setOnHidden {
            // cleaning up
            pubs.clean()
        }
    }

    init {
        this.title = "Room Finder"
        pubs = PublicRoomsView(publicRoomList, account )
        root.apply {
            this.minWidth = 600.0
            VBox.setVgrow(this, Priority.ALWAYS)
            padding = Insets(10.0)
            add(pubs.ui)
        }
    }
}
