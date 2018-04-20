package koma.util.collect

import java.util.*

/**
 * bidirectional mapping between ordered keys and ordered ranges
 * ranges are expected to be continuous and non-overlapping
 */
class KeyRangeMap<K: Comparable<K>, V>{
    private val keyMap = TreeMap<K, Entry>()
    private val posMap = TreeMap<Int, Entry>()

    inner class Entry(
            val key: K,
            val value: V,
            val range: IntRange
    )

    fun containsKey(key: K): Boolean = keyMap.containsKey(key)

    fun insert(key: K, size: Int, value: V) {
        if (size < 0) {
            throw IllegalArgumentException("Can't map range with negative size")
        }
        if (keyMap.containsKey(key)) {
            throw IllegalArgumentException("Duplicate key in bidirectional range map")
        }
        val lower = keyMap.lowerEntry(key)
        val offset = lower?.value?.range?.end ?: 0
        val range = IntRange(offset, offset + size)
        val entry = Entry(key, value, range)
        keyMap[key] = entry
        if (size > 0) {
            prepareTail(key, size)
            registerEntryPos(entry)
        }
    }

    fun remove(key: K) {
        if (!keyMap.containsKey(key)) {
            throw IllegalArgumentException("Can't remove key that's not included")
        }
        val removedSize = keyMap[key]!!.range.size
        prepareTail(key, -removedSize) // backward
        keyMap.remove(key)
    }

    fun changeSizeOf(key: K, delta: Int) {
        val entry = keyMap[key]
        entry ?: throw IllegalArgumentException("Attempting to resize range that doesn't exist")
        val range = entry.range
        if(delta == 0) return
        range.changeSize(delta)
        prepareTail(key, delta)
        registerEntryPos(entry)
    }

    fun getEntryByKey(key: K): Entry? = keyMap[key]

    fun getEntryAt(pos: Int): Entry {
        val floor = posMap.floorEntry(pos)
        if (floor == null) {
            throw IndexOutOfBoundsException("No floor entry for the given pos")
        }
        val entry = floor.value
        val range = entry.range
        if (!range.contains(pos)) {
            throw IndexOutOfBoundsException("Position is not in the range of the floor entry")
        }
        return entry
    }

    fun getEntryOrNull(pos: Int): Entry? {
        val floor = posMap.floorEntry(pos)
        floor ?: return null
        return floor.value
    }

    fun getOffsetFor(key: K): Int? = keyMap[key]?.range?.start

    private fun prepareTail(key: K, delta: Int) {
        val ceiling = keyMap.ceilingEntry(key)
        ceiling ?: return
        val fromPos = ceiling.value.range.start
        // including the entry key points to, if it exists
        posMap.removeTail(fromPos)
        // starting from the next one
        keyMap.tailMap(key, false)
                .forEach{k: K, vr: Entry ->
                    vr.range.shift(delta)
                    registerEntryPos(vr)
                }
    }

    private fun registerEntryPos(entry: Entry) {
        val r = entry.range
        if (r.isNotEmpty()) {
            posMap.put(r.start, entry)
        }
    }

    private fun checkConsistency() {
        assert(keyMap.size == posMap.size,
                { "keymap size ${keyMap.size} posMap size ${posMap.size}"})
        var last = 0
        for ((km, pm) in keyMap.entries.zip(posMap.entries)) {
            val entry = km.value
            assert(km.key == entry.key)
            assert(entry == pm.value)
            assert(pm.key == entry.range.start)
            assert(entry.range.start == last)
            last = entry.range.end
        }
    }
}

class IntRange(
        start: Int,
        // exclusive
        end: Int
) {
    var start = start
        private set
    var end = end
        private set

    val size
        get() = end - start

    init {
        if (end < start) {
            throw IllegalArgumentException("IntRange can't have negative size")
        }
    }

    fun shift(delta: Int) {
        start += delta
        end += delta
    }

    fun changeSize(delta: Int) {
        if (delta < 0 && end + delta < start) {
            throw IllegalArgumentException("Can't reduce IntRange size to negative")
        }
        end += delta
    }

    fun contains(pos: Int): Boolean = pos in start..(end - 1)

    fun isNotEmpty(): Boolean = end > start

    override fun toString(): String {
        return "IntRange[$start,$end)"
    }
}

fun<K, V> TreeMap<K, V>.removeTail(fromKey: K) {
    val hkeys = this.tailMap(fromKey, true).keys.toList()
    hkeys.forEach { this.remove(it) }
}
