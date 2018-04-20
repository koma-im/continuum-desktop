package koma.storage.message.file

import koma.matrix.room.naming.RoomId
import koma.storage.config.config_paths
import koma.util.time.utcDateFrom
import java.nio.file.Path

fun get_log_path(key: Long, roomId: RoomId): Path? {
    val date = utcDateFrom(key)
    val year = "%04d".format(date.year)
    val month = "%02d".format(date.monthValue)
    val day = "%02d".format(date.dayOfMonth)
    val dir = disc_save_path(
            roomId.servername,
            roomId.localstr,
            year, month, day)
    return dir?.resolve(key.toString())
}

fun disc_save_path(vararg paths: String): Path? {
    val f = config_paths.getCreateDir(
            "discussion", *paths
    )
    return f?.toPath()
}
