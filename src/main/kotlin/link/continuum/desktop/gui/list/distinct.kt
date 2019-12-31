package link.continuum.desktop.gui.list

import javafx.collections.FXCollections
import javafx.collections.ObservableList
import link.continuum.desktop.util.debugAssertUiThread
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * not synchronized, use on the UI thread
 */
class DedupList<T, U>(private val identify: (T)->U) {
    private val rwList = FXCollections.observableArrayList<T>()
    private val elementSet = mutableSetOf<U>()
    val list: ObservableList<T> = FXCollections.unmodifiableObservableList(rwList)
    fun getIds(): List<U> {
        return elementSet.toList()
    }
    init {
    }
    fun add(element: T) {
        debugAssertUiThread()
        if (elementSet.add(identify(element))) {
            rwList.add(element)
        }
    }
    fun addIfAbsent(id: U, compute: (U)->T) {
        debugAssertUiThread()
        if (!elementSet.add(id)) {
            return
        }
        rwList.add(compute(id))
    }
    fun size() = rwList.size
    fun addAll(elements: Collection<T>) {
        debugAssertUiThread()
        rwList.addAll(elements.filter { elementSet.add(identify(it)) })
    }
    fun addAll(index: Int, elements: List<T>) {
        debugAssertUiThread()
        rwList.addAll(index, elements.filter { elementSet.add(identify(it)) })
    }
    fun remove(element: T) {
        debugAssertUiThread()
        logger.debug { "remove $element"}
        if (elementSet.remove(identify(element))) {
            rwList.remove(element)
        }
    }
    fun removeById(id: U) {
        debugAssertUiThread()
        if (elementSet.remove(id)) {
            rwList.removeIf { identify(it) == id }
        }
    }
    fun removeAll(elements: Collection<T>) {
        debugAssertUiThread()
        rwList.removeAll(elements.filter {
            elementSet.remove(identify(it))
        })
    }
    fun removeAllById(ids: Collection<U>) {
        debugAssertUiThread()
        val rm = ids.toSet()
        val oldSize = elementSet.size
        elementSet.minusAssign(rm)
        val newSize = elementSet.size
        if (newSize == oldSize) {
            return
        }
        rwList.removeAll {
            rm.contains(identify(it))
        }
    }
}
