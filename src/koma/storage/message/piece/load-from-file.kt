package koma.storage.message.piece

import com.squareup.moshi.JsonEncodingException
import com.squareup.moshi.Moshi
import koma.matrix.UserIdAdapter
import koma.matrix.event.parse
import koma.matrix.room.naming.RoomId
import koma.matrix.sync.RawMessage
import util.getCreateAppDataDir
import java.io.File

fun loadStoredDiscussion(roomId: RoomId): List<DiscussionPiece> {
    val dir = getCreateAppDataDir(
            "discussion",
            roomId.servername,
            roomId.localstr,
            create = false)
    dir ?: return listOf()
    val f = File(dir)
    return f.walk().filter { !it.isDirectory }.mapNotNull { it.loadDiscussion() }.toList()
}

private fun File.loadDiscussion(): DiscussionPiece? {
    val time = this.name.toLongOrNull()
    time?: return null
    var following: String? = null
    val moshi = Moshi.Builder().add(UserIdAdapter()).build()
    val adapter = moshi.adapter(RawMessage::class.java)
    val messages = this.readLines().mapNotNull {
        if (it.startsWith("#")) {
            if (it.startsWith("# following_event ")) {
                following = it.substringAfter("# following_event ").trim()
            }
            null
        } else {
            try {
                adapter.fromJson(it)?.parse()
            } catch (e: JsonEncodingException) {
                println("failed to load line $it")
                return@mapNotNull null
            }
        }
    }.toMutableList()
    val res = DiscussionPiece(
            messages = messages, timekey = time)
    res.following_event = following
    res.filename = this.absolutePath
    res.savedHash = res.contentHash()
    return res
}
