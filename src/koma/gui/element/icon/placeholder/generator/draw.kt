package koma.gui.element.icon.placeholder.generator

import javafx.geometry.VPos
import javafx.scene.SnapshotParameters
import javafx.scene.canvas.Canvas
import javafx.scene.image.Image
import javafx.scene.paint.Color
import javafx.scene.text.Font
import javafx.scene.text.FontWeight
import javafx.scene.text.TextAlignment
import koma.gui.element.icon.user.extract_key_chars
import koma_app.appState

fun getImageForName(name: String, bgcolor: Color): Image {
    val scale = appState.koma.appSettings.settings.scaling
    val isize = 32.0 * scale
    val canva = Canvas(isize, isize)
    val graphc = canva.graphicsContext2D
    val fgcolor = Color.WHITE
    graphc.setFill(bgcolor)
    val arcsize = isize * 0.3
    graphc.fillRoundRect(0.0, 0.0, isize, isize, arcsize, arcsize)
    graphc.setFill(fgcolor)
    graphc.textAlign = TextAlignment.CENTER
    graphc.textBaseline = VPos.CENTER
    graphc.font = Font.font("serif", FontWeight.BOLD, 20.0 * scale)
    val middle = isize * 0.5
    val mid0 = isize * 0.25
    val mid1 = isize * 0.75
    val keychars = extract_key_chars(name)
    graphc.fillText(keychars.first.toString(), mid0, middle)
    graphc.fillText(keychars.second.toString(), mid1, middle)
    val params =SnapshotParameters()
    params.fill = Color.TRANSPARENT
    val im = canva.snapshot(params, null)
    return im
}


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
