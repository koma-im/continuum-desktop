package link.continuum.desktop.util

import java.util.AbstractList

/**
 * A List-like implementation that is exceedingly efficient for the purposes
 * of the VirtualFlow. Typically there is not much variance in the number of
 * cells -- it is always some reasonably consistent number. Yet for efficiency
 * in code, we like to use a linked list implementation so as to append to
 * start or append to end. However, at times when we need to iterate, LinkedList
 * is expensive computationally as well as requiring the construction of
 * temporary iterators.
 *
 *
 * This linked list like implementation is done using an array. It begins by
 * putting the first item in the center of the allocated array, and then grows
 * outward (either towards the first or last of the array depending on whether
 * we are inserting at the head or tail). It maintains an index to the start
 * and end of the array, so that it can efficiently expose iteration.
 *
 *
 * This class is package private solely for the sake of testing.
 */
internal class ArrayLinkedList<T> : AbstractList<T>() {
    /**
     * The array list backing this class. We default the size of the array
     * list to be fairly large so as not to require resizing during normal
     * use, and since that many ArrayLinkedLists won't be created it isn't
     * very painful to do so.
     */
    private val array: ArrayList<T?> = ArrayList(50)

    private var firstIndex = -1
    private var lastIndex = -1

    val first: T?
        get() = if (firstIndex == -1) null else array[firstIndex]

    val last: T?
        get() = if (lastIndex == -1) null else array[lastIndex]

    init {

        for (i in 0..49) {
            array.add(null)
        }
    }

    fun addFirst(cell: T?) {
        // if firstIndex == -1 then that means this is the first item in the
        // list and we need to initialize firstIndex and lastIndex
        if (firstIndex == -1) {
            lastIndex = array.size / 2
            firstIndex = lastIndex
            array.set(firstIndex, cell)
        } else if (firstIndex == 0) {
            // we're already at the head of the array, so insert at position
            // 0 and then increment the lastIndex to compensate
            array.add(0, cell)
            lastIndex++
        } else {
            // we're not yet at the head of the array, so insert at the
            // firstIndex - 1 position and decrement first position
            array.set(--firstIndex, cell)
        }
    }

    fun addLast(cell: T) {
        // if lastIndex == -1 then that means this is the first item in the
        // list and we need to initialize the firstIndex and lastIndex
        if (firstIndex == -1) {
            lastIndex = array.size / 2
            firstIndex = lastIndex
            array[lastIndex] = cell
        } else if (lastIndex == array.size - 1) {
            // we're at the end of the array so need to "add" so as to force
            // the array to be expanded in size
            array.add(++lastIndex, cell)
        } else {
            array[++lastIndex] = cell
        }
    }

    override val size: Int
        get() = if (firstIndex == -1) 0 else lastIndex - firstIndex + 1

    override fun isEmpty(): Boolean {
        return firstIndex == -1
    }

    override fun get(index: Int): T? {
        return if (index > lastIndex - firstIndex || index < 0) {
            // Commented out exception due to RT-29111
            // throw new java.lang.ArrayIndexOutOfBoundsException();
            null
        } else array[firstIndex + index]

    }

    override fun clear() {
        for (i in array.indices) {
            array.set(i, null)
        }

        lastIndex = -1
        firstIndex = lastIndex
    }

    fun removeFirst(): T? {
        return if (isEmpty()) null else removeAt(0)
    }

    fun removeLast(): T? {
        return if (isEmpty()) null else removeAt(lastIndex - firstIndex)
    }

    override fun removeAt(index: Int): T {
        if (index > lastIndex - firstIndex || index < 0) {
            throw ArrayIndexOutOfBoundsException()
        }

        // if the index == 0, then we're removing the first
        // item and can simply set it to null in the array and increment
        // the firstIndex unless there is only one item, in which case
        // we have to also set first & last index to -1.
        if (index == 0) {
            val cell = array[firstIndex]
            array.set(firstIndex, null)
            if (firstIndex == lastIndex) {
                lastIndex = -1
                firstIndex = lastIndex
            } else {
                firstIndex++
            }
            return cell!!
        } else if (index == lastIndex - firstIndex) {
            // if the index == lastIndex - firstIndex, then we're removing the
            // last item and can simply set it to null in the array and
            // decrement the lastIndex
            val cell = array[lastIndex]
            array.set(lastIndex--, null)
            return cell!!
        } else {
            // if the index is somewhere in between, then we have to remove the
            // item and decrement the lastIndex
            val cell = array[firstIndex + index]
            array.set(firstIndex + index, null)
            for (i in firstIndex + index + 1..lastIndex) {
                array[i - 1] = array[i]
            }
            array.set(lastIndex--, null)
            return cell!!
        }
    }
}
