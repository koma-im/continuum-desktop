package koma.gui.element.icon.placeholder.generator

import javafx.scene.paint.Color
import koma.koma_app.appState
import koma.storage.persistence.settings.AppSettings
import mu.KotlinLogging

/* get a color that's not too bright
 */
fun hashStringColorDark(str: String): Color {
    val ash: Int = str.hashCode()
    val r: Int = ash.and(0xFF0000).ushr(16)
    val g = ash.and(0x00FF00).ushr(8)
    val b = ash.and(0x0000FF)
    val c = Color.rgb(r, g, b)
    return if (c.brightness > 0.9) c.darker() else c
}
