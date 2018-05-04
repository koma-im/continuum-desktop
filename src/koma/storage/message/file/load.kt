package koma.storage.message.file

import koma.matrix.event.EventId
import koma.matrix.json.MoshiInstance
import koma.storage.message.piece.Segment
import koma.util.common.eprintln
import koma.util.common.tryParse
import java.nio.file.Path



fun loadDiscussion(key: Long, path: Path): Segment? {
    val adapter = MoshiInstance.roomEventAdapter
    val file = path.toFile()
    val lines = file.readLines()
    val messages = lines.filterNot { it.startsWith("#") }.mapNotNull {
        try {
            adapter.fromJson(it)
        } catch (e: Exception) {
            println("failed to load line $it: $e")
            return@mapNotNull null
        }
    }
    if (messages.size < 1) {
        System.err.println("warning: deleting unexpected empty segment $path")
        if (file.exists())
            file.delete()
        return null
    }

    val res = Segment(path, key, messages)
    val metaline = lines.last()
    if (metaline.startsWith("# metadata ")) {
        val json = metaline.substringAfter("# metadata ").trim()
        Segment.Metadata.adapter.tryParse(json).fold({
            res.meta = it
        }, {
            eprintln("Failed to parse metadata $it")
        })
    } else if (metaline.startsWith("# following_event ")) {
        res.meta.following_event = EventId(metaline.substringAfter("# following_event ").trim())
    } else if (!metaline.startsWith("{")) {
        eprintln("Incorrect line $metaline")
    }

    res.savedHash = res.hashCode()
    return res
}
