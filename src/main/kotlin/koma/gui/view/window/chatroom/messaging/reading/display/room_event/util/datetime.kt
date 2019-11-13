package koma.gui.view.window.chatroom.messaging.reading.display.room_event.util

import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.layout.Priority
import javafx.scene.text.Text
import link.continuum.desktop.gui.HBox
import link.continuum.desktop.gui.add
import java.text.SimpleDateFormat
import java.util.*

class DatatimeView {
    val root = Text().apply {
        opacity = 0.4
    }
    fun updateTime(time: Long) {
        root.text = SimpleDateFormat("MM-dd HH:mm").format(time)
    }
    init {
    }
}
