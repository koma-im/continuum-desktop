package koma.gui

import javafx.stage.Screen
import javafx.stage.Stage
import org.h2.mvstore.MVMap
import org.h2.mvstore.MVStore
import kotlin.time.ExperimentalTime
import kotlin.time.measureTimedValue

fun save_win_geometry(stage: Stage, kvStore: MVStore) {
    val prefs = kvStore.openMap<String, Double>("window-size-settings")
    prefs.put("chat-stage-x", stage.x)
    prefs.put("chat-stage-y", stage.y)
    prefs.put("chat-stage-w", stage.width)
    prefs.put("chat-stage-h", stage.height)
}

@ExperimentalTime
fun setSaneStageSize(stage: Stage, kvStore: MVStore) {
    val (prefs, t) = measureTimedValue {
        kvStore.openMap<String, Double>("window-size-settings")
    }
    val sx = prefs.get("chat-stage-x")
    val sy = prefs.get("chat-stage-y")
    setWidthHeight(stage, prefs)
    if (sx != null && sy != null) {
        stage.x = sx
        stage.y = sy
    } else
        stage.centerOnScreen()
}

@ExperimentalTime
private fun setWidthHeight(stage: Stage, prefs: MVMap<String, Double>) {
    val (sw, t) = measureTimedValue {
        prefs.get("chat-stage-w")
    }
    val sh = prefs.get("chat-stage-h")
    if (sw != null  && sh != null) {
        stage.width = sw
        stage.height = sh
    } else {
        val rec = Screen.getPrimary().getVisualBounds()
        val width = rec.width
        val height = rec.height
        stage.width = width / 2.0
        stage.height = height / 2.0
    }
}

