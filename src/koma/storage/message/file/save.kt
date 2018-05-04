package koma.storage.message.file

import com.squareup.moshi.JsonDataException
import koma.storage.message.piece.Segment
import koma.util.common.eprintln

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
            eprintln("Failed to encode $mesg: $e")
        }
    }
    try {
        writer.append("# metadata ")
        val info = Segment.Metadata.adapter.toJson(seg.meta)
        writer.append(info)
        writer.append("\n")
    } catch (e: JsonDataException) {
        eprintln("Failed to encode ${seg.meta}: $e")
    }
    writer.close()
    seg.savedHash = list.hashCode()
}
