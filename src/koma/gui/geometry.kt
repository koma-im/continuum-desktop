package koma.gui

import javafx.stage.Stage
import view.ChatView
import java.util.prefs.Preferences

fun save_win_geometry(stage: Stage) {
    val prefs = Preferences.userNodeForPackage(ChatView::class.java)
    prefs.putDouble("chat-stage-x", stage.x)
    prefs.putDouble("chat-stage-y", stage.y)
    prefs.putDouble("chat-stage-w", stage.width)
    prefs.putDouble("chat-stage-h", stage.height)
}

fun setSaneStageSize(stage: Stage) {
    val prefs = Preferences.userNodeForPackage(ChatView::class.java)

    val sx = prefs.getDouble("chat-stage-x", -1.0)
    val sy = prefs.getDouble("chat-stage-y", -1.0)
    if (sx > 0 && sy > 0) {
        stage.x = sx
        stage.y = sy
    } else
        stage.centerOnScreen()
    val sw = prefs.getDouble("chat-stage-w", -1.0)
    val sh = prefs.getDouble("chat-stage-h", -1.0)
    if (sw > 0 && sh > 0) {
        stage.width = sw
        stage.height = sh
    } else
        stage.sizeToScene()
}

