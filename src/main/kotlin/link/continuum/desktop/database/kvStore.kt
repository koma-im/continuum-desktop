package link.continuum.desktop.database

import javafx.stage.Stage
import koma.storage.persistence.settings.encoding.ProxyList
import org.h2.mvstore.MVStore

/**
 * consolidate operations to reduce risk of getting string keys wrong
 */
class KeyValueStore(
        private val mvStore: MVStore
) {
    val windowSizeMap = mvStore.openMap<String, Double>("window-size-settings")
    private val map1 = mvStore.openMap<String, String>("strings")
    private val proxies = mvStore.openMap<String,Long>("proxies")
    val activeAccount = Entry("active-account", map1)
    val proxyList = ProxyList(proxies)
    fun saveStageSize(stage: Stage) {
        val prefs = windowSizeMap
        prefs.put("chat-stage-x", stage.x)
        prefs.put("chat-stage-y", stage.y)
        prefs.put("chat-stage-w", stage.width)
        prefs.put("chat-stage-h", stage.height)
    }
    fun close() {
        mvStore.close()
    }
}

class Entry<K, V: Any>(
        private val key: K,
        private val map: MutableMap<K, V>
) {
    fun getOrNull(): V?{
        return map.get(key)
    }
    fun put(value: V) {
        map.put(key, value)
    }
    fun remove() {
        map.remove(key)
    }
}