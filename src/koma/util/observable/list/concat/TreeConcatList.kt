package koma.util.observable.list.concat

import javafx.collections.ListChangeListener
import javafx.collections.ObservableList
import javafx.collections.ObservableListBase
import koma.util.collect.KeyRangeMap
import java.util.*

/**
 * don't modify directly
 */
class TreeConcatList<K: Comparable<K>, E>: ObservableListBase<E>(), ObservableList<E> {
    private val keyRangeMap = KeyRangeMap<K, ObservableList<E>>()
    private val keyToListener = HashMap<K, Listener>()

    override var size: Int = 0
        private set

    private inner class Listener(private val key: K): ListChangeListener<E> {
        override fun onChanged(c: ListChangeListener.Change<out E>) {
            this@TreeConcatList.sourceChanged(key, c)
        }
    }

    fun containsKey(key: K): Boolean = keyRangeMap.containsKey(key)
    class SubIndex<K>(val key: K, val subindex: Int)
    fun locateKey(pos: Int): SubIndex<K>? {
        val entry = keyRangeMap.getEntryOrNull(pos)
        entry ?: return null
        return SubIndex(entry.key, pos - entry.range.start)
    }

    override fun get(index: Int): E {
        val entry = keyRangeMap.getEntryAt(index)
        val list = entry.value
        val subindex = index - entry.range.start
        assert(entry.range.contains(index), { "Index $index not within ${entry.range}" })
        assert(subindex >= 0 && subindex < list.size,
                { "Index $subindex out of sub list size ${list.size}"})
        return list[subindex]
    }

    fun put(key: K, list: ObservableList<E>) {
        if (keyRangeMap.containsKey(key)) {
            throw IllegalArgumentException("Can't concat lists with same key")
        }
        val listener = Listener(key)
        list.addListener(listener)
        val p = keyToListener.put(key, listener)
        assert(p == null, { "Duplicate listener for key $key"} )
        beginChange()
        size += list.size
        keyRangeMap.insert(key, list.size, list)
        val entry = keyRangeMap.getEntryByKey(key)!!
        val range = entry.range
        nextAdd(range.start, range.end)
        endChange()
    }

    fun remove(key: K): ObservableList<E>? {
        val entry = keyRangeMap.getEntryByKey(key)
        entry ?: return null
        val list = entry.value
        val listener = keyToListener[key] ?: error("No listener for key $key")
        beginChange()
        list.removeListener(listener)
        size -= list.size
        val r = entry.range
        nextRemove(r.start, list)
        keyRangeMap.remove(key)
        endChange()
        return list
    }

    /**
     * Called when a change from the source is triggered.
     * @param c the change
     */
    private fun sourceChanged(key: K, c: ListChangeListener.Change<out E>) {
        beginChange()
        while (c.next()) {
            if (c.wasPermutated()) {
                permutate(key, c)
            } else if (c.wasUpdated()) {
                update(key, c)
            } else {
                addRemove(key, c)
            }
        }
        endChange()
    }

    private fun permutate(key: K, c: ListChangeListener.Change<out E>) {
        val offset = keyRangeMap.getOffsetFor(key)!!
        if (c.from < c.to) {
            val perm = (c.from..(c.to - 1))
                    .map { c.getPermutation(it) }
                    .map { it + offset }
                    .toIntArray()
            val from = c.from + offset
            val to = c.to + offset
            nextPermutation(from, to, perm)
        }
    }

    private fun addRemove(key: K, c: ListChangeListener.Change<out E>) {
        val offset = keyRangeMap.getOffsetFor(key)!!
        val from = offset + c.from
        val sizeChange = c.addedSize - c.removedSize
        keyRangeMap.changeSizeOf(key, sizeChange)
        size += sizeChange
        nextRemove(from, c.removed)
        nextAdd(from, from + c.addedSize)
    }

    private fun update(key: K, c: ListChangeListener.Change<out E>) {
        val offset = keyRangeMap.getOffsetFor(key)!!
        (c.from until c.to).forEach {
            nextUpdate(it + offset)
        }
    }

    private fun checkConsistency() {
        val keys = keyToListener.keys.sorted()
        var start = 0
        for (key in keys) {
            assert(keyRangeMap.containsKey(key))
            val entry = keyRangeMap.getEntryByKey(key)!!
            assert(entry.range.size == entry.value.size)
            assert(start == entry.range.start)
            assert(start == keyRangeMap.getOffsetFor(key))
            assert(keyRangeMap.getEntryOrNull(start) == entry)
            assert(keyRangeMap.getEntryOrNull(start + entry.value.size - 1) == entry)
            assert(keyRangeMap.getEntryOrNull(start + entry.value.size) != entry)
            start = entry.range.end
        }
    }

    // override methods that modify
    // not complete
    override fun add(element: E): Boolean {
        throw UnsupportedOperationException()
    }

    override fun add(index: Int, element: E) {
        throw UnsupportedOperationException()
    }

    override fun addAll(elements: Collection<E>): Boolean {
        throw UnsupportedOperationException()
    }

    override fun addAll(vararg elements: E): Boolean {
        throw UnsupportedOperationException()
    }

    override fun addAll(index: Int, elements: Collection<E>): Boolean {
        throw UnsupportedOperationException()
    }

    override fun clear() {
        throw UnsupportedOperationException()
    }

    override fun remove(from: Int, to: Int) {
        throw UnsupportedOperationException()
    }
    override fun remove(element: E): Boolean {
        throw UnsupportedOperationException()
    }
}
