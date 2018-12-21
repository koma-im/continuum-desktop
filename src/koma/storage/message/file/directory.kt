package koma.storage.message.file

import koma.koma_app.SaveToDiskTasks
import koma.matrix.room.naming.RoomId
import koma.storage.config.ConfigPaths
import koma.storage.message.piece.Segment
import java.nio.file.Path
import java.util.*

/**
 * what are available on disk
 */
class SegmentsDirectory(private val roomId: RoomId, val paths: ConfigPaths){
    private val map by lazy { findEntries() }

    fun put(key: Long, value: Segment) {
        map.put(key, InMemory(value))
    }

    operator fun get(key: Long): Segment? {
        val v = map.get(key)
        v ?: return null
        return when (v) {
            is InMemory -> v.segment
            is OnDisk -> tryLoad(v.key, v.path)
        }
    }

    private fun tryLoad(key: Long, path: Path): Segment? {
        val s = loadDiscussion(key, path)
        if (s != null) {
            map[key] = InMemory(s)
            return s
        }
        System.err.println("Failed to load segment at $path")
        map.remove(key)
        return null
    }

    init {
        SaveToDiskTasks.addJob {
            for (item in this.map.values) {
                if (item is InMemory) {
                    item.segment.save()
                }
            }
        }
    }

    /**
     * skip files that fail to load
     */
    private fun getFirstAvailable(load: ()->DirItem?, limit: Int=5): Segment? {
        for (_i in 1..limit) {
            val item = load()
            when (item) {
                null -> return null
                is InMemory -> return item.segment
                is OnDisk -> {
                    val s = tryLoad(item.key, item.path)
                    if (s!= null) return s
                }
            }
        }
        return null
    }
    fun last(): Segment? {
        val call = { this.map.lastEntry()?.value }
        return getFirstAvailable(call)
    }
    fun higher(key: Long, inclusive: Boolean): Segment? {
        val call = { this.map.let {
            if (inclusive) it.ceilingEntry(key)
            else it.higherEntry(key)
        }?.value }
        return getFirstAvailable(call)
    }

    fun lower(key: Long, inclusive: Boolean): Segment? {
        val call = { this.map.let {
            if (inclusive) it.floorEntry(key)
            else it.lowerEntry(key)
        }?.value}
        return getFirstAvailable(call)
    }


    private fun findEntries(): TreeMap<Long, DirItem> {
        val t = TreeMap<Long, DirItem>()
        val dir = paths.disc_save_path(
                roomId.servername,
                roomId.localstr)
        dir ?: return t
        val f = dir.toFile()
        f.walk().filter { !it.isDirectory }
                .forEach {
                    val ts = it.name.toLongOrNull()
                    if (ts!= null) {
                        t[ts] = OnDisk(it.toPath(), ts)
                    }
                }
        return t
    }
}

private sealed class DirItem
private class OnDisk(val path: Path, val key: Long): DirItem()
private class InMemory(val segment: Segment): DirItem()
