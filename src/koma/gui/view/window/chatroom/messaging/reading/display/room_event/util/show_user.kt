package koma.gui.view.window.chatroom.messaging.reading.display.room_event.util

import javafx.geometry.Pos
import javafx.scene.Node
import koma.gui.element.icon.AvatarAlways
import koma.koma_app.appState
import koma.matrix.UserId
import tornadofx.*

fun showUser(node: Node, userId: UserId) {
    val user = appState.store.userStore.getOrCreateUserId(userId)
    node.apply {
        hbox(spacing = 5.0) {
            add(AvatarAlways(user.avatar, user.name, user.color))
            vbox {
                alignment = Pos.CENTER
                label(user.name) {
                    minWidth = 50.0
                    maxWidth = 100.0
                    textFill = user.color
                }
            }
        }
    }
}
