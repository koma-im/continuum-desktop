package link.continuum.desktop.gui.icon.avatar

import com.sun.javafx.tk.Toolkit
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.layout.Background
import javafx.scene.layout.BackgroundFill
import javafx.scene.layout.CornerRadii
import javafx.scene.layout.HBox
import javafx.scene.paint.Color
import javafx.scene.text.Font
import javafx.scene.text.FontWeight
import javafx.scene.text.Text
import koma.koma_app.appState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import mu.KotlinLogging
import tornadofx.*
import java.util.*

private val logger = KotlinLogging.logger {}

@ExperimentalCoroutinesApi
class InitialIcon(
        private val iSize: Double = appState.store.settings.scaling * 32.0
) {
    private val radii =  CornerRadii(iSize * 0.2)
    private var color: Color? = null

    private val charL = Text().apply { style { fill = Color.WHITE } }
    private val charR = Text().apply { style { fill = Color.WHITE } }

    val root = HBox().apply {
        val s = iSize.px
        style {
            minWidth = s
            minHeight = s
            maxWidth = s
            maxHeight = s
        }
        alignment = Pos.CENTER
        vbox {
            alignment = Pos.CENTER_RIGHT
            add(charL)
        }
        vbox {
            minWidth = 0.05 * iSize
        }
        vbox {
            alignment = Pos.CENTER_LEFT
            add(charR)
        }
    }
    init {
        val font = Font.font("serif", FontWeight.BOLD, 0.5 * iSize)
        charL.font = font
        charR.font = font
    }

    fun show() {
        this.root.isManaged = true
        this.root.isVisible = true
    }

    fun hide() {
        this.root.isManaged = false
        this.root.isVisible = false
    }

    fun updateItem(charL: Char, charR: Char, color: Color) {
        Toolkit.getToolkit().checkFxUserThread()
        this.color = color
        root.background = backgrounds.computeIfAbsent(color) {
            logger.debug { "initial icon $charL $charR $color" }
            Background(BackgroundFill(it, radii, Insets.EMPTY))
        }
        this.charL.text = charL.toString()
        this.charR.text = charR.toString()
    }

    companion object {
        private val backgrounds = WeakHashMap<Color, Background>()
    }
}
