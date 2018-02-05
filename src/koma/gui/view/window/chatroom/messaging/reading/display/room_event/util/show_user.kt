package koma.gui.view.window.chatroom.messaging.reading.display.room_event.util

import javafx.geometry.Pos
import javafx.scene.Node
import koma.matrix.UserId
import koma.storage.users.UserStore
import tornadofx.*

fun showUser(node: Node, userId: UserId) {
    val user = UserStore.getOrCreateUserId(userId)
    node.apply {
        hbox(spacing = 5.0) {
            imageview(user.avatarImgProperty)
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
