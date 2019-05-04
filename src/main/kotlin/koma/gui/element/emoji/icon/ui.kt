package koma.gui.element.emoji.icon

import com.vdurmont.emoji.EmojiManager
import javafx.beans.property.SimpleStringProperty
import javafx.beans.value.ObservableValue
import javafx.event.ActionEvent
import javafx.geometry.Pos
import javafx.scene.Cursor
import javafx.scene.control.*
import javafx.scene.control.skin.LabeledSkinBase
import javafx.scene.image.ImageView
import javafx.scene.layout.StackPane
import javafx.scene.text.Font
import javafx.scene.text.Text
import koma.koma_app.appState
import link.continuum.libutil.`?or`
import mu.KotlinLogging
import tornadofx.*

private val logger = KotlinLogging.logger {}

class EmojiIcon (
        size: Double
) {
    private val imageView = ImageView()
    private val text = Text().apply {
        val imageAvail= booleanBinding(imageView.imageProperty()) { value != null }
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