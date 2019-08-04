package koma.gui.element.control

import javafx.beans.InvalidationListener
import javafx.beans.Observable
import javafx.beans.WeakInvalidationListener
import javafx.beans.property.SimpleObjectProperty
import javafx.collections.ListChangeListener
import javafx.collections.ObservableList
import javafx.collections.WeakListChangeListener
import javafx.scene.AccessibleAttribute
import javafx.scene.control.FocusModel
import javafx.scene.control.ListView
import javafx.scene.control.Skin
import koma.gui.element.control.skin.KListViewSkin
import koma.gui.element.control.skin.KVirtualFlow
import koma.gui.element.emoji.keyboard.NoSelectionModel
import tornadofx.*
import java.lang.ref.WeakReference



class KListView<T>: ListView<T> {
    constructor(): super()
    constructor(items: ObservableList<T>): super(items)
    val visibleIndexRange = SimpleObjectProperty<NullableIndexRange>()
    var flow: KVirtualFlow<*>? = null
    init {
        this.selectionModel = NoSelectionModel()
        this.focusModel = KListViewFocusModel(this)
        this.skinProperty().onChange { it?.let  { onSkin(it) }}
    }
    private fun onSkin(skin: Skin<*>) {
        if (skin !is KListViewSkin<*>) return
        flow = skin.getVFlow()
        val f = skin.getVFlow()
        visibleIndexRange.cleanBind(f.visibleIndexRange)
    }
    override fun createDefaultSkin(): Skin<*> {
        return KListViewSkin(this)
    }
}

class NullableIndexRange(
        val start: Int?,
        val endInclusive: Int?
) {
    override fun equals(other: Any?): Boolean =
            other is NullableIndexRange && (start == other.start && endInclusive == other.endInclusive)

    override fun toString(): String = "$start..$endInclusive"

    override fun hashCode(): Int {
        var result = start ?: 0
        result = 31 * result + (endInclusive ?: 0)
        return result
    }
}

internal class KListViewFocusModel<T>(private val listView: ListView<T>) : FocusModel<T>() {
    private var itemCount = 0

    private val itemsObserver: InvalidationListener

    // Listen to changes in the listview items list, such that when it
    // changes we can update the focused index to refer to the new indices.
    private val itemsContentListener = ListChangeListener<T>{ c: ListChangeListener.Change<out T> ->
        updateItemCount()

        while (c.next()) {
            // looking at the first change
            val from = c.getFrom()

            if (c.wasReplaced() || c.getAddedSize() == getItemCount()) {
                updateDefaultFocus()
                return@ListChangeListener
            }

            if (focusedIndex == -1 || from > focusedIndex) {
                return@ListChangeListener
            }

            c.reset()
            var added = false
            var removed = false
            var addedSize = 0
            var removedSize = 0
            while (c.next()) {
                added = added or c.wasAdded()
                removed = removed or c.wasRemoved()
                addedSize += c.getAddedSize()
                removedSize += c.getRemovedSize()
            }

            if (added && !removed) {
                focus(Math.min(getItemCount() - 1, focusedIndex + addedSize))
            } else if (!added && removed) {
                focus(Math.max(0, focusedIndex - removedSize))
            }
        }
    }

    private val weakItemsContentListener = WeakListChangeListener<T>(itemsContentListener)

    private val isEmpty: Boolean
        get() = itemCount == -1

    init {
        itemsObserver = object : InvalidationListener {
            private var weakItemsRef = WeakReference(listView.items)

            override fun invalidated(observable: Observable) {
                val oldItems = weakItemsRef.get()
                weakItemsRef = WeakReference(listView.items)
                updateItemsObserver(oldItems, listView.items)
            }
        }
        this.listView.itemsProperty().addListener(WeakInvalidationListener(itemsObserver))
        if (listView.items != null) {
            this.listView.items.addListener(weakItemsContentListener)
        }

        updateItemCount()
        updateDefaultFocus()

        focusedIndexProperty().addListener { _ -> listView.notifyAccessibleAttributeChanged(AccessibleAttribute.FOCUS_ITEM) }
    }


    private fun updateItemsObserver(oldList: ObservableList<T>?, newList: ObservableList<T>?) {
        // the listview items list has changed, we need to observe
        // the new list, and remove any observer we had from the old list
        oldList?.removeListener(weakItemsContentListener)
        newList?.addListener(weakItemsContentListener)

        updateItemCount()
        updateDefaultFocus()
    }

    override fun getItemCount(): Int {
        return itemCount
    }

    override fun getModelItem(index: Int): T? {
        if (isEmpty) return null
        return if (index < 0 || index >= itemCount) null else listView.items[index]

    }

    private fun updateItemCount() {
        val items = listView.items
        itemCount = items?.size ?: -1
    }

    private fun updateDefaultFocus() {
        // when the items list totally changes, we should clear out
        // the focus
        var newValueIndex = -1
        if (listView.items != null) {
            val focusedItem = focusedItem
            if (focusedItem != null) {
                newValueIndex = listView.items.indexOf(focusedItem)
            }

            // we put focus onto the first item, if there is at least
            // one item in the list
            if (newValueIndex == -1) {
                val s = listView.items.size
                newValueIndex = if (s > 0) s - 1 else -1
            }
        }

        focus(newValueIndex)
    }
}
