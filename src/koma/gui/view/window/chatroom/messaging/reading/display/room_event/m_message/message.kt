package koma.gui.view.window.chatroom.messaging.reading.display.room_event.m_message

import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.input.Clipboard
import javafx.scene.layout.Priority
import javafx.scene.layout.StackPane
import javafx.scene.paint.Color
import koma.gui.view.window.chatroom.messaging.reading.display.room_event.m_message.content.render_node
import koma.gui.view.window.chatroom.messaging.reading.display.room_event.util.showDatetime
import koma.matrix.event.room_message.MRoomMessage
import koma.storage.users.UserStore
import tornadofx.*

fun renderMessageFromUser(item: MRoomMessage): Node {
    val sus = UserStore.getOrCreateUserId(item.sender)
    val sender = sus.displayName
    val avtar = sus.avatarImgProperty
    val color = sus.color
    val _node = StackPane()

    _node.apply {
        paddingAll = 2.0
        hbox {
            minWidth = 1.0
            prefWidth = 1.0
            style {
                alignment = Pos.CENTER_LEFT
                paddingAll = 2.0
                backgroundColor = multi(Color.WHITE)
            }
            vbox {
                imageview(avtar) {
                    isCache = true
                    isPreserveRatio = true
                }
            }

            vbox(spacing = 2.0) {
                lazyContextmenu {
                    item("Copy text").action {
                        item.content?.body?.let {
                            Clipboard.getSystemClipboard().putString(it)
                        }
                    }
                }
                hgrow = Priority.ALWAYS
                hbox(spacing = 10.0) {
                    hgrow = Priority.ALWAYS
                    text(sender) {
                        fill = color
                    }

                    showDatetime(this, item.origin_server_ts)
                }
                hbox(spacing = 5.0) {
                    val n = item.render_node()
                    add(n)
                }
            }
        }
    }
    return _node
}


