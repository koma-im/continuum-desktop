package koma.gui.view.roomsview

import controller.guiEvents
import javafx.beans.property.Property
import javafx.scene.layout.HBox
import model.Room
import rx.javafx.kt.actionEvents
import rx.javafx.kt.addTo
import tornadofx.*


fun addMenu(node: HBox, room: Property<Room>) {
    node.contextmenu {
        item("leave") {
            actionEvents().map { room.value }.addTo(guiEvents.leaveRoomRequests)
        }
        item("upload icon") {
            actionEvents().map { room.value }.addTo(guiEvents.uploadRoomIconRequests)
        }
        item("Add alias") {
            actionEvents().map { room.value }.addTo(guiEvents.putRoomAliasRequests)
        }
        item("Set alias") {
            actionEvents().map { room.value }.addTo(guiEvents.renameRoomRequests)
        }
    }
}

