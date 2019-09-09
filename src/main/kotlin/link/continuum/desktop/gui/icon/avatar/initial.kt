package link.continuum.desktop.gui.icon.avatar

import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.layout.*
import javafx.scene.paint.Color
import javafx.scene.text.Font
import javafx.scene.text.Text
import link.continuum.desktop.gui.add
import link.continuum.desktop.gui.vbox
import mu.KotlinLogging
import java.util.*
import kotlin.streams.toList

private val logger = KotlinLogging.logger {}

class InitialIcon(
        private val iSize: Double
) {
    private val radii =  CornerRadii(iSize * 0.2)
    private var color: Color? = null

    private val charL = Text().apply {
        fill = Color.WHITE
    }
    private val charR = Text().apply { fill = Color.WHITE }
    private val charC = Text().apply { fill = Color.WHITE }

    private val two = HBox().apply {
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
    val root = StackPane().apply {
        val s = iSize
        minWidth = s
        minHeight = s
        maxWidth = s
        maxHeight = s
        children.add(two)
        children.add(HBox().apply {
            alignment = Pos.CENTER
            children.add(charC)
        })
    }
    init {
        val font = Font.font("sans", 0.5 * iSize)
        charL.font = font
        charR.font = font
        charC.font = Font.font("sans", 0.9 * iSize)
    }

    fun show() {
        this.root.isManaged = true
        this.root.isVisible = true
    }

    fun hide() {
        this.root.isManaged = false
        this.root.isVisible = false
    }

    fun updateItem(charL: String, charR: String, color: Color) {
        this.color = color
        root.background = backgrounds.computeIfAbsent(color) {
            logger.debug { "initial icon $charL $charR $color" }
            Background(BackgroundFill(it, radii, Insets.EMPTY))
        }
        charC.text = ""
        this.charL.text = charL
        this.charR.text = charR
    }

    fun updateCenter(char: String, color: Color) {
        this.color = color
        root.background = backgrounds.computeIfAbsent(color) {
            logger.debug { "initial icon $charL $charR $color" }
            Background(BackgroundFill(it, radii, Insets.EMPTY))
        }
        this.charL.text = ""
        this.charR.text = ""
        charC.text = char
    }

    fun updateItem(input: String, color: Color) {
        val (c1, c2) = extractKeyChar(input)
        if (c2 != null) {
            updateItem(c1, c2, color)
        } else {
            updateCenter(c1, color)
        }
    }

    companion object {
        private val backgrounds = WeakHashMap<Color, Background>()
    }
}

internal fun extractKeyChar(input: String): Pair<String, String?> {
    val trim = input.replace("(IRC)", "").trim()
    val cps = trim.codePoints().toList()
    val ideo = cps.find { Character.isIdeographic(it)  }
    if (ideo != null) {
        return String(Character.toChars(ideo)) to null
    }
    val first = cps.firstOrNull()?.let { String(Character.toChars(it)) } ?: ""
    val i2 = cps.indexOfFirst { Character.isSpaceChar(it)  }.let { if (it < 0) null else it + 1} ?: 1
    val second = cps.getOrNull(i2)?.let { String(Character.toChars(it))  }
    return first to second
}
