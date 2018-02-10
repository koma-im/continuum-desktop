package koma.storage.message.piece

import java.util.*

open class Concatenater<K, E: Comparable<E>, P: OrderedListPart<K,E>>(val list: MutableList<E>) {
    protected val pieces = TreeMap<K, Positioned<P>>()

    protected class Positioned<P>(
            val item: P,
            /**
             * location in the merged list
             */
            var pos: Int
    )

    protected fun lowerEntryValue(key: K): P? {
        return this.pieces.lowerEntry(key)?.value?.item
    }

    protected fun higherEntryValue(key: K): P? {
        return this.pieces.higherEntry(key)?.value?.item
    }

    @Synchronized
    fun insertPiece(piece: P): Boolean {
        val key = piece.getKey()
        val newlist = piece.getList()

        val prev = this.lowerEntryValue(key)?.getList()
        if (prev != null) {
            val prevlast = prev.last()
            if (prevlast >= newlist.first()) {
                // check for duplicate elements
                newlist.removeAll {
                    if (it < prevlast) true
                    else if (it > prevlast) false
                    else prev.contains(it)
                }
            }
        }

        val next = this.higherEntryValue(key)?.getList()
        if (next != null) {
            val nextfirst = next.first()
            if (newlist.size >0 && newlist.last() >= nextfirst ) {
                newlist.removeAll {
                    if (it > nextfirst) true
                    else if (it < nextfirst) false
                    else next.contains(it)
                }
            }
        }

        if (this.pieces.containsKey(key)) {
            val existing = this.pieces.get(key)!!.item.getList()
            val ne = newlist.filter { !existing.contains(it) }
            insertOrderlyInto(key, ne)
            return false
        }

        if (newlist.size > 0) {
            val lower = this.pieces.lowerEntry(key)?.value
            val pos = lower?.let { it.pos + it.item.getList().size } ?: 0
            this.pieces.put(key, Positioned(piece, pos))
            this.list.addAll(pos, newlist)
            shiftForward(this.pieces.higherKey(key), newlist.size)
            return true
        }
        return false
    }

    protected fun shiftForward(key: K?, shift: Int) {
        key ?: return
        this.pieces.tailMap(key).forEach { _, u -> u.pos += shift }
    }

    @Synchronized
    protected fun insertOrderlyInto(key: K, elements: List<E>): Boolean {
        val target = this.pieces.get(key)
        target?: return false
        val targetList = target.item.getList()
        val oldlen = targetList.size
        for (e in elements) {
            val i = targetList.posInOrder(e)
            targetList.add(i, e)
            list.add(i + target.pos, e)
        }
        val shift = targetList.size - oldlen
        shiftForward(this.pieces.higherKey(key), shift)
        return true
    }

    protected fun insertAllIntoAt(key: K, ind: Int, elements: List<E>) {
        val dest= this.pieces.get(key)
        dest ?: return
        val destList = dest.item.getList()
        destList.addAll(ind, elements)
        list.addAll(dest.pos + ind, elements)
        shiftForward(this.pieces.higherKey(key), elements.size)
    }

    @Synchronized
    fun insertIntoLast(elements: List<E>): Boolean {
        val lastKey = this.pieces.lastKey()
        lastKey?: return false
        return insertOrderlyInto(lastKey, elements)
    }
}

private fun<E: Comparable<E>> List<E>.posInOrder(e: E): Int {
    val i = this.indexOfLast { e >= it }
    return if (i > -1) i + 1 else 0
}
