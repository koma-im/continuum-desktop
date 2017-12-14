package koma.storage.message.piece

import koma.matrix.room.naming.RoomId
import util.getCreateAppDataDir
import java.io.File
import java.util.*

fun DiscussionPiece.save() {
    if (messages.size == 0 && filename != null) {
        val file = File(filename)
        if (file.exists())
            file.delete()
        return
    }
    if (!needSave()) {
        return
    }
    if (filename == null) {
        println("storage location not set: ${timekey}")
    }
    if (this.messages.isEmpty()) {
        println("tried to save empty messages to ${filename}")
        return
    }
    val file = File(filename)
    file.writeText("")
    val writer = file.bufferedWriter()
    for (mesg in this.messages) {
        val line = mesg.original.toJson()
        writer.append(line)
        writer.append('\n')
    }
    this.following_event?.let { writer.append("# following_event " + it + "\n") }
    writer.close()
    savedHash = messages.hashCode()
}


@Synchronized
fun DiscussionPiece.set_log_path(time: Long, roomId: RoomId) {
    val datetime = Date(time)
    val year = "%04d".format(datetime.year + 1900)
    val month = "%02d".format(datetime.month)
    val day = "%02d".format(datetime.day)
    val dir = getCreateAppDataDir(
            "discussion",
            roomId.servername,
            roomId.localstr,
            year, month, day)
    val path = dir?.let { it + File.separator + time.toString() }
    filename = path
}

fun DiscussionPiece.first_event_id(): String? =
        this.messages.firstOrNull()?.original?.event_id
