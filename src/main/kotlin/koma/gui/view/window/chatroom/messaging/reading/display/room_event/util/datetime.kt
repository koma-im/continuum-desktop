package koma.gui.view.window.chatroom.messaging.reading.display.room_event.util

import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.text.Text
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

class DatatimeView {
    val root = HBox()
    private val text = Text().apply {
        opacity = 0.4
    }
    fun updateTime(time: Long) {
        text.text = SimpleDateFormat("MM-dd HH:mm").format(time)
    }
    init {
        with(root) {
            hgrow = Priority.ALWAYS
            alignment = Pos.CENTER_RIGHT
            add(text)
        }
    }
}
