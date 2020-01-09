package koma.gui

import javafx.stage.Screen
import javafx.stage.Stage
import link.continuum.desktop.database.KeyValueStore
import org.h2.mvstore.MVMap
import kotlin.time.ExperimentalTime
import kotlin.time.measureTimedValue

@ExperimentalTime
fun setSaneStageSize(stage: Stage, kvStore: KeyValueStore) {
    val prefs = kvStore.windowSizeMap
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

