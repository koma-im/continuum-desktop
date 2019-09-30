package koma.gui.element.emoji.icon

import com.vdurmont.emoji.EmojiManager
import javafx.geometry.Pos
import javafx.scene.Cursor
import javafx.scene.control.Hyperlink
import javafx.scene.control.Tooltip
import javafx.scene.image.ImageView
import link.continuum.desktop.gui.StackPane
import javafx.scene.text.Font
import javafx.scene.text.Text
import link.continuum.desktop.gui.add
import link.continuum.desktop.gui.cleanBind
import link.continuum.desktop.gui.removeWhen
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

class EmojiIcon (
        size: Double
) {
    private val imageView = ImageView()
    private val text = Text().apply {
        val imageAvail= imageView.imageProperty().isNotNull
        removeWhen { imageAvail }
        font = Font(size)
    }

    val node = Hyperlink().apply {
        styleClass.clear()
        cursor = Cursor.DEFAULT
        graphic = StackPane().apply {
            add(this@EmojiIcon.text)
            alignment = Pos.BASELINE_CENTER
            add(imageView)
        }
        tooltip = Tooltip("emoji").apply {
            font = Font.font(size)
        }
    }

    constructor(emoji: String, size: Double): this(size) {
        val imp =  EmojiCache.getEmoji(emoji)
        this.imageView.imageProperty().cleanBind(imp)
    }

    var emoji: String = ""

    fun updateEmoji(symbol: String) {
        emoji = symbol
        text.text = symbol
        val imp = EmojiCache.getEmoji(symbol)
        this.imageView.imageProperty().cleanBind(imp)
        node.tooltip.text = getEmojiDescription(symbol)
    }
}


private fun getEmojiDescription(emoji: String): String {
    val emo = EmojiManager.getByUnicode(emoji)?.description
    if (emo != null){
        return emo
    }
    val code = getEmojiCode(emoji)
    logger.debug { "unknown emoji $emoji with code $code" }
    return code
}