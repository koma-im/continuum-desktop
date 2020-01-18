package koma.gui.view.window.roomfinder

import javafx.geometry.Insets
import javafx.scene.Scene
import javafx.scene.layout.Priority
import javafx.stage.Stage
import koma.gui.view.window.roomfinder.publicroomlist.PublicRoomsView
import koma.koma_app.AppData
import link.continuum.desktop.gui.JFX
import link.continuum.desktop.gui.VBox
import link.continuum.desktop.gui.add
import link.continuum.desktop.util.Account

class RoomFinder(
        account: Account,
        appData: AppData
) {
    val root = VBox(5.0)
    val pubs: PublicRoomsView
    private val stage = Stage()
    fun open() {
        stage.show()
    }

    init {
        stage.title = "Room Finder"
        stage.initOwner(JFX.primaryStage)
        stage.scene = Scene(root)
        pubs = PublicRoomsView(account, appData)
        stage.setOnHidden {
        }
        root.apply {
            this.minWidth = 600.0
            VBox.setVgrow(this, Priority.ALWAYS)
            padding = Insets(10.0)
            add(pubs.ui)
        }
    }
}
