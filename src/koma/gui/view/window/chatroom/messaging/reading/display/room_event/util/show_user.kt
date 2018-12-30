package koma.gui.view.window.chatroom.messaging.reading.display.room_event.util

import javafx.geometry.Pos
import javafx.scene.Node
import koma.gui.element.icon.AvatarAlways
import koma.matrix.UserId
import koma.koma_app.appState
import tornadofx.*

fun showUser(node: Node, userId: UserId) {
    val user = appState.userStore.getOrCreateUserId(userId)
    node.apply {
        hbox(spacing = 5.0) {
            add(AvatarAlways(user.avatarURL, user.displayName, user.color))
            vbox {
                alignment = Pos.CENTER
                label(user.displayName) {
                    minWidth = 50.0
                    maxWidth = 100.0
                    textFill = user.color
                }
            }
        }
    }
}
