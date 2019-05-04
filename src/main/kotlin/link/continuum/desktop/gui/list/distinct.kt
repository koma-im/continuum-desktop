package link.continuum.desktop.gui.list

import javafx.collections.FXCollections

/**
 * not synchronized, use on the UI thread
 */
class DedupList<T, U>(private val identify: (T)->U) {
    private val rwList = FXCollections.observableArrayList<T>()
    private val elementSet = mutableSetOf<U>()
    val list = FXCollections.unmodifiableObservableList(rwList)
    fun getIds(): List<U> {
        return elementSet.toList()
    }
    init {

    }
    fun add(element: T) {
        if (elementSet.add(identify(element))) {
            rwList.add(element)
        }
    }
    fun addIfAbsent(id: U, compute: (U)->T) {
        if (!elementSet.add(id)) {
            return
        }
        rwList.add(compute(id))
    }
    fun size() = rwList.size
    fun addAll(elements: List<T>) {
        rwList.addAll(elements.filter { elementSet.add(identify(it)) })
    }
    fun addAll(index: Int, elements: List<T>) {
        rwList.addAll(index, elements.filter { elementSet.add(identify(it)) })
    }
    fun remove(element: T) {
        if (elementSet.remove(identify(element))) {
            rwList.remove(element)
        }
    }
    fun removeById(id: U) {
        if (elementSet.remove(id)) {
            rwList.removeIf { identify(it) == id }
        }
    }
}
