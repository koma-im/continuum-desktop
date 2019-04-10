package link.continuum.desktop.gui.list

import javafx.collections.FXCollections

class DedupList<T, U>(private val identify: (T)->U) {
    private val rwList = FXCollections.observableArrayList<T>()
    private val elementSet = mutableSetOf<U>()
    val list = FXCollections.unmodifiableObservableList(rwList)
    init {

    }
    fun add(element: T) {
        if (elementSet.add(identify(element))) {
            rwList.add(element)
        }
    }
    fun size() = rwList.size
    fun addAll(elements: List<T>) {
        rwList.addAll(elements.filter { elementSet.add(identify(it)) })
    }
}
