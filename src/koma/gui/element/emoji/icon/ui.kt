package koma.gui.element.emoji.icon

import javafx.beans.property.SimpleStringProperty
import javafx.beans.value.ObservableValue
import javafx.event.ActionEvent
import javafx.geometry.Pos
import javafx.scene.control.ButtonBase
import javafx.scene.control.Label
import javafx.scene.control.Skin
import javafx.scene.control.skin.LabeledSkinBase
import javafx.scene.image.ImageView
import javafx.scene.layout.StackPane
import javafx.scene.text.Font
import javafx.scene.text.Text
import koma.koma_app.appState
import koma.storage.persistence.settings.AppSettings
import tornadofx.*

class EmojiIcon (
        private val settings: AppSettings = appState.store.settings
): ButtonBase() {

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
        Label()
        return EmojiIconSkin(this)
    }

    val size = settings.fontSize
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
        val pane = StackPane()
        val imageAvail= booleanBinding(imageView.imageProperty()) { value != null }
        text.removeWhen { imageAvail }
        text.font = Font(size)
        pane.add(text)
        pane.alignment = Pos.BASELINE_CENTER
        pane.add(imageView)
        this.graphic = pane
    }

    private fun bindImage(emoji: ObservableValue<String>) {
        val imp =  emoji.select { char -> EmojiCache.getEmoji(char) }
        this.imageView.imageProperty().cleanBind(imp)
    }
}

private class EmojiIconSkin(icon: EmojiIcon)
    : LabeledSkinBase<EmojiIcon>(icon)
