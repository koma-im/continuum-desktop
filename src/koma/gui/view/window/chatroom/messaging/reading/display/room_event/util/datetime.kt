package koma.gui.view.window.chatroom.messaging.reading.display.room_event.util

import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.layout.Priority
import tornadofx.*
import java.text.SimpleDateFormat
import java.util.*

fun showDatetime(node: Node, ts: Long) {
    val datetime= Date(ts)
    node.apply {
        hbox {
            hgrow = Priority.ALWAYS
            text(SimpleDateFormat("MM-dd HH:mm").format(datetime)) {
                opacity = 0.4
                alignment = Pos.CENTER_RIGHT
            }
        }
    }
}
