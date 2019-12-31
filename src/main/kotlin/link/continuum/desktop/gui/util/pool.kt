package link.continuum.desktop.gui.util

import link.continuum.desktop.util.debugAssertUiThread

/**
 * use on UI thread
 */
class UiPool<T>(private val factory: ()->T) {
    private val pool = mutableListOf<T>()
    fun take(): T {
        debugAssertUiThread()
        val i = pool.lastIndex
        if (i < 0)  return factory()
        val obj = pool.removeAt(pool.lastIndex)
        return obj
    }

    fun pushBack(value: T) {
        debugAssertUiThread()
        pool.add(value)
    }
}

class Recyclable<T>(private val pool: UiPool<T>) {
    var value: T? = null
    fun get() = value ?: pool.take()
    fun recycle() {
        value?.let {pool.pushBack(it)}
    }
}