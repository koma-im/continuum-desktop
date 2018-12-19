package koma.gui.element.emoji.keyboard

import com.vdurmont.emoji.EmojiManager
import javafx.beans.property.SimpleListProperty
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.scene.Node
import javafx.scene.control.*
import javafx.scene.layout.Pane
import javafx.scene.layout.VBox
import javafx.scene.text.Font
import javafx.util.Callback
import koma.gui.element.control.PrettyListView
import koma.gui.element.emoji.category.emojiCategories
import koma.gui.element.emoji.icon.EmojiIcon
import koma_app.appState
import org.controlsfx.control.PopOver
import tornadofx.*

data class EmojiCategoryInRows(
        val name: String,
        val rows: ObservableList<List<String>>
)

private fun<T> List<T>.groupEvery(n: Int): List<List<T>> {
    var listOfLists = mutableListOf<List<T>>()
    for (i in 0..this.size / n) {
        if (i * n >= this.size)
            break
        listOfLists.add(this.subList(i*n, kotlin.comparisons.minOf(this.size, (i+1)*n)))
    }
    return listOfLists
}

private fun getEmojiCategoryRows(): List<EmojiCategoryInRows> {
    return emojiCategories.map { cat ->
        val rows = cat.emojis.groupEvery(8)
        EmojiCategoryInRows(cat.name, FXCollections.observableArrayList(rows))
    }
}

object EmojiKeyboard {

    private val root = VBox()

    private val stage = PopOver(root)
    private val emojicategoryrowlist = SimpleListProperty<List<String>>()
    var onEmojiChosen: ((String)->Unit)? = null

    init {
        root.maxHeight = 150.0
        stage.isDetachable = false
        val categories = getEmojiCategoryRows()
        emojicategoryrowlist.set(categories[0].rows)
        with(root) {
            val tg = ToggleGroup()
            hbox {
                for (cat in categories) {
                    val rows = cat.rows
                    val first = rows[0][0]
                    togglebutton {
                        style {
                            paddingAll = 0.0
                        }
                        toggleGroup = tg
                        tooltip = Tooltip(cat.name)
                        graphic = EmojiIcon(first)
                        action {
                            emojicategoryrowlist.set(cat.rows)
                        }
                    }
                }
            }

            val rows = PrettyListView<List<String>>()
            with(rows) {
                items = emojicategoryrowlist
                cellFactory = object: Callback<ListView<List<String>>, ListCell<List<String>>> {
                    override fun call(param: ListView<List<String>>?): ListCell<List<String>> {
                        return EmojiRowCell({ onEmojiClicked(it)})
                    }
                }
                selectionModel = NoSelectionModel()
            }
            add(rows)
        }
    }

    private fun onEmojiClicked(emojiSymbol: String) {
        onEmojiChosen?.invoke(emojiSymbol)
    }

    class EmojiRowCell(val cb: (em: String) -> Unit) : ListCell<List<String>>() {
        private val icons = mutableListOf<EmojiIcon>()

        init {
            graphic = Pane()
            for (i in 0..8) {
                val node = EmojiIcon()
                val tip = Tooltip()
                tip.font = Font.font(appState.koma.appSettings.fontSize)
                node.tooltip = (tip)
                node.action { node.emojiProperty.get()?.let{cb(it)} }
                node.relocate(node.size * 1.1 * i, 0.0)
                icons.add(node)
                graphic.add(node)
            }
        }

        override fun updateItem(item: List<String>?, empty: Boolean) {
            super.updateItem(item, empty)

            if (item != null) {
                item.forEachIndexed { index, emojiSymbol ->
                    val icon = icons[index]
                    icon.setEmoji(emojiSymbol)
                    icon.tooltip?.text = getEmojiDescription(emojiSymbol)
                }
            }
        }
    }

    fun show(own: Node) {
        stage.show(own)
    }
}

private fun getEmojiDescription(emoji: String): String? {
    return EmojiManager.getByUnicode(emoji)?.description
}

class NoSelectionModel<T>(): MultipleSelectionModel<T>() {
    override fun getSelectedIndices(): ObservableList<Int> = FXCollections.emptyObservableList()
    override fun getSelectedItems(): ObservableList<T> = FXCollections.emptyObservableList()
    override fun selectIndices(index: Int, vararg indices: Int) {}
    override fun selectAll() {}
    override fun selectFirst() {}
    override fun selectLast() {}
    override fun clearAndSelect(index: Int) {}
    override fun clearSelection() {}
    override fun clearSelection(index: Int) {}
    override fun select(index: Int) {}
    override fun select(obj: T) {}
    override fun isSelected(index: Int): Boolean = false
    override fun isEmpty(): Boolean = true
    override fun selectPrevious() {}
    override fun selectNext() {}
}
