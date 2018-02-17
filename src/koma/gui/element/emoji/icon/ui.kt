package koma.gui.element.emoji.icon

import com.sun.javafx.scene.control.behavior.ButtonBehavior
import com.sun.javafx.scene.control.skin.LabeledSkinBase
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.value.ObservableValue
import javafx.event.ActionEvent
import javafx.scene.Group
import javafx.scene.control.ButtonBase
import javafx.scene.control.Skin
import javafx.scene.image.ImageView
import javafx.scene.text.Font
import javafx.scene.text.Text
import koma.gui.element.emoji.category.EmojiSymbol
import koma.storage.config.settings.AppSettings
import tornadofx.*

class EmojiIcon (): ButtonBase() {

    constructor(emoji: ObservableValue<EmojiSymbol>): this() {
        bindEmojiSymbol(emoji)
    }

    constructor(emoji: EmojiSymbol): this(SimpleObjectProperty(emoji))

    override fun fire() {
        if (!isDisabled()) {
            fireEvent(ActionEvent());
        }
    }

    override fun createDefaultSkin(): Skin<*> {
        return EmojiIconSkin(this)
    }

    val size = AppSettings.fontSize
    val emojiProperty = SimpleObjectProperty<EmojiSymbol>()

    private val imageView = ImageView()
    private val text = Text()

    fun setEmoji(symbol: EmojiSymbol) {
        bindEmojiSymbol(SimpleObjectProperty(symbol))
    }

    fun bindEmojiSymbol(symbol: ObservableValue<EmojiSymbol>) {
        emojiProperty.cleanBind(symbol)
        val char = stringBinding(symbol) { value.glyph }
        text.textProperty().bind(char)
        bindImage(symbol)
    }

    init {
        val pane = Group()
        val imageAvail= booleanBinding(imageView.imageProperty()) { value != null }
        text.removeWhen { imageAvail }
        text.font = Font(size)
        pane.add(text)
        pane.add(imageView)
        this.graphic = pane
    }

    private fun bindImage(emoji: ObservableValue<EmojiSymbol>) {
        val imp =  emoji.select { url -> EmojiCache.getEmoji(url.codepoint) }
        this.imageView.imageProperty().cleanBind(imp)
    }
}

private class EmojiIconSkin(icon: EmojiIcon)
    : LabeledSkinBase<EmojiIcon, ButtonBehavior<EmojiIcon>>(
        icon, ButtonBehavior(icon)
)
