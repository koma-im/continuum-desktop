package koma.storage.message.file

import com.squareup.moshi.JsonDataException
import koma.storage.message.piece.Segment

fun Segment.save() {
    val seg = this
    val list = seg.list
    val file = path.toFile()
    if (!seg.needSave()) {
        return
    }
    file.writeText("")
    val writer = file.bufferedWriter()
    for (mesg in list) {
        try {
            val line = mesg.toJson()
            writer.append(line)
            writer.append('\n')
        } catch (e: JsonDataException) {
        }
    }
    seg.following_event?.let { writer.append("# following_event " + it + "\n") }
    writer.close()
    seg.savedHash = list.hashCode()
}
