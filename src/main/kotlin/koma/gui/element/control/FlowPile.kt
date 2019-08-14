package koma.gui.element.control

import javafx.scene.control.ListCell
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * recycle ListCell in a ListView
 */
interface CellPool<I, T>
        where I: ListCell<T>{
    fun clear()
    fun size(): Int
    fun removeFirst(predicate: (I)-> Boolean, item: T?): I?
    fun removeLastOrNull(item: T?): I?
    fun add (cell: I)
    fun find(item: T?, predicate: (I)-> Boolean): I?
    fun firstOrNull(item: T?): I?
    fun forEach(action: (I) -> Unit)
}
class EmptyCellPool<I: ListCell<T>, T>: CellPool<I, T> {
    override fun clear() {}
    override fun size(): Int = 0
    override fun removeFirst(predicate: (I) -> Boolean, item: T?): I? = null
    override fun removeLastOrNull(item: T?): I? = null
    override fun add(cell: I) {}
    override fun find(item: T?, predicate: (I) -> Boolean): I? = null
    override fun firstOrNull(item: T?): I? = null
    override fun forEach(action: (I) -> Unit) {}

}
class SimpleCellPool<I: ListCell<T>, T>: CellPool<I, T> {
    private val pile = mutableListOf<I>()
    override fun clear() = pile.clear()
    override fun size(): Int {
        return pile.size
    }
    override fun removeFirst(predicate: (I)-> Boolean, item: T?): I? {
        val i = pile.indexOfFirst(predicate)
        if (i < 0 ) return null
        return pile.removeAt(i)
    }

    override fun removeLastOrNull( item: T?): I? {
        if (pile.isEmpty()) return null
        return pile.removeAt(pile.size - 1)
    }

    override fun add (cell: I) {
        logger.trace { "recycling cell $cell" }
        pile.add(cell)
    }

    override fun find(item: T?, predicate: (I)-> Boolean): I? {
        return pile.find(predicate)
    }

    override fun firstOrNull(item: T?): I? {
        return pile.firstOrNull()
    }
    override fun forEach(action: (I) -> Unit) {
        pile.forEach(action)
    }
}
