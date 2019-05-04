package koma.gui.element.emoji.keyboard

import com.vdurmont.emoji.EmojiManager
import javafx.beans.property.SimpleListProperty
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.control.*
import javafx.scene.layout.Pane
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox
import javafx.scene.text.Font
import javafx.util.Callback
import koma.gui.element.control.PrettyListView
import koma.gui.element.emoji.category.emojiCategories
import koma.gui.element.emoji.icon.EmojiIcon
import koma.koma_app.appState
import koma.storage.persistence.settings.AppSettings
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import link.continuum.desktop.gui.UiDispatcher
import mu.KotlinLogging
import org.controlsfx.control.PopOver
import tornadofx.*

private val logger = KotlinLogging.logger {}

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

private typealias EmojiHandler = (String)->Unit

@ExperimentalCoroutinesApi
class EmojiKeyboard {

    private val root = VBox()

    private val stage = PopOver(root)
    private val emojicategoryrowlist = SimpleListProperty<List<String>>()
    var onEmoji: EmojiHandler? = null

    init {
        val size: Double = appState.store.settings.fontSize
        root.maxHeight = 150.0
        root.maxWidth = size * 10.2
        stage.isDetachable = false
        val categories = getEmojiCategoryRows()
        emojicategoryrowlist.set(categories[0].rows)
        with(root) {
            val tg = ToggleGroup()
            hbox {
                hgrow = Priority.ALWAYS
                alignment = Pos.CENTER
                for (cat in categories) {
                    val rows = cat.rows
                    val first = rows[0][0]
                    togglebutton("") {
                        alignment = Pos.CENTER
                        styleClass.clear()
                        this.padding = Insets(size*0.1)
                        toggleGroup = tg
                        tooltip = Tooltip(cat.name)
                        graphic = EmojiIcon(first, size).node
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
                        return EmojiRowCell({
                            logger.trace { "emoji selected $it" }
                            onEmoji?.invoke(it)
                        }, size)
                    }
                }
                selectionModel = NoSelectionModel()
            }
            add(rows)
        }
    }

    class EmojiRowCell(
            onEmoji: EmojiHandler,
            size: Double
            ) : ListCell<List<String>>() {
        private val icons = mutableListOf<EmojiIcon>()
        private val pane = Pane()
        init {
            for (i in 0..7) {
                val icon = EmojiIcon(size)
                val node = icon.node
                node.action {
                    logger.trace { "emoji icon action" }
                    onEmoji(icon.emoji)
                }
                node.relocate(size * 1.1 * i, 0.0)
                icons.add(icon)
                pane.add(node)
            }
        }

        override fun updateItem(item: List<String>?, empty: Boolean) {
            super.updateItem(item, empty)

            if (empty || item == null) {
                graphic = null
                return
            }
            item.zip(icons).forEach { (code, icon) ->
                icon.updateEmoji(code)
            }
            graphic = pane
        }
    }

    fun show(own: Node) {
        stage.show(own)
    }
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
