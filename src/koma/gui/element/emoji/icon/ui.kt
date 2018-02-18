package koma.gui.element.emoji.icon

import com.sun.javafx.scene.control.behavior.ButtonBehavior
import com.sun.javafx.scene.control.skin.LabeledSkinBase
import javafx.beans.property.SimpleStringProperty
import javafx.beans.value.ObservableValue
import javafx.event.ActionEvent
import javafx.scene.Group
import javafx.scene.control.ButtonBase
import javafx.scene.control.Skin
import javafx.scene.image.ImageView
import javafx.scene.text.Font
import javafx.scene.text.Text
import koma.storage.config.settings.AppSettings
import tornadofx.*

class EmojiIcon (): ButtonBase() {

    constructor(emoji: ObservableValue<String>): this() {
        bindEmojiSymbol(emoji)
    }

    constructor(emoji: String): this(SimpleStringProperty(emoji))

    override fun fire() {
        if (!isDisabled()) {
            fireEvent(ActionEvent());
        }
    }

    override fun createDefaultSkin(): Skin<*> {
        return EmojiIconSkin(this)
    }

    val size = AppSettings.fontSize
    val emojiProperty = SimpleStringProperty()

    private val imageView = ImageView()
    private val text = Text()

    fun setEmoji(symbol: String) {
        bindEmojiSymbol(SimpleStringProperty(symbol))
    }

    fun bindEmojiSymbol(symbol: ObservableValue<String>) {
        emojiProperty.cleanBind(symbol)
        text.textProperty().bind(symbol)
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

    private fun bindImage(emoji: ObservableValue<String>) {
        val imp =  emoji.select { char -> EmojiCache.getEmoji(char) }
        this.imageView.imageProperty().cleanBind(imp)
    }
}

private class EmojiIconSkin(icon: EmojiIcon)
    : LabeledSkinBase<EmojiIcon, ButtonBehavior<EmojiIcon>>(
        icon, ButtonBehavior(icon)
)
