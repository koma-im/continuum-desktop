package koma.storage.message.file

import koma.matrix.event.EventId
import koma.matrix.json.MoshiInstance
import koma.storage.config.config_paths
import koma.storage.message.piece.Segment
import java.nio.file.Path



fun loadDiscussion(key: Long, path: Path): Segment? {
    var following: EventId? = null
    val adapter = MoshiInstance.roomEventAdapter
    val file = path.toFile()
    val messages = file.readLines().mapNotNull {
        if (it.startsWith("#")) {
            if (it.startsWith("# following_event ")) {
                following = EventId(it.substringAfter("# following_event ").trim())
            }
            null
        } else {
            try {
                adapter.fromJson(it)
            } catch (e: Exception) {
                println("failed to load line $it: $e")
                return@mapNotNull null
            }
        }
    }
    if (messages.size < 1) {
        System.err.println("warning: deleting unexpected empty segment $path")
        if (file.exists())
            file.delete()
        return null
    }
    val res = Segment(path, key, messages)
    res.following_event = following
    res.savedHash = res.contentHash()
    return res
}
