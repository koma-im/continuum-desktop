package koma.storage.message.piece

import koma.matrix.room.naming.RoomId
import java.io.File
import java.time.Instant
import java.time.ZoneOffset

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
        println("storage location not set:  ${timekey} ${this.messages}")
        return
    }
    if (this.messages.isEmpty()) {
        println("tried to save empty messages to ${filename}")
        return
    }
    val file = File(filename)
    file.writeText("")
    val writer = file.bufferedWriter()
    for (mesg in this.messages) {
        val line = mesg.toJson()
        writer.append(line)
        writer.append('\n')
    }
    this.following_event?.let { writer.append("# following_event " + it + "\n") }
    writer.close()
    savedHash = messages.hashCode()
}


@Synchronized
fun DiscussionPiece.set_log_path(time: Long, roomId: RoomId) {
    val date = Instant.ofEpochMilli(time).atOffset(ZoneOffset.UTC).toLocalDate()
    val year = "%04d".format(date.year)
    val month = "%02d".format(date.monthValue)
    val day = "%02d".format(date.dayOfMonth)
    val dir = disc_save_path(
            roomId.servername,
            roomId.localstr,
            year, month, day)
    val path = dir?.let { it + File.separator + time.toString() }
    filename = path
}

fun DiscussionPiece.first_event_id(): String? =
        this.messages.firstOrNull()?.event_id
