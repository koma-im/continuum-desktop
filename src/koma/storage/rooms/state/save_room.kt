package koma.storage.rooms.state

import koma.matrix.event.room_message.state.RoomPowerLevelsContent
import koma.matrix.json.MoshiInstance
import koma.matrix.room.naming.RoomAlias
import koma.matrix.room.participation.RoomJoinRules
import koma.matrix.room.visibility.HistoryVisibility
import koma.matrix.room.visibility.RoomVisibility
import koma.model.user.UserState
import koma.storage.config.ConfigPaths
import model.Room
import java.io.File
import java.io.IOException

val statefilename = "room_state.json"
val usersfilename = "users.txt"

fun ConfigPaths.state_save_path(vararg paths: String): String? {
    return getOrCreate("state", *paths)
}


fun ConfigPaths.saveRoom(room: Room){
    val dir = state_save_path(
            room.id.servername,
            room.id.localstr)
    dir?: return
    val data = SavedRoomState(
            room.aliases,
            room.visibility,
            room.joinRule,
            room.histVisibility,
            room.name.get(),
            room.iconURL.toString(),
            room.power_levels
    )
    val moshi = MoshiInstance.moshi
    val jsonAdapter = moshi.adapter(SavedRoomState::class.java).indent("    ")
    val json = try {
        jsonAdapter.toJson(data)
    } catch (e: ClassCastException) {
        e.printStackTrace()
        return
    }
    val path = dir + File.separator + statefilename
    try {
        val file = File(path)
        file.writeText(json)
    } catch (e: IOException) {
    }
    save_room_members(dir, room.members.sorted())
}

class SavedRoomState (
    val aliases: List<RoomAlias>,
    val visibility: RoomVisibility,
    val join_rule: RoomJoinRules,
    val history_visibility: HistoryVisibility,
    val name: String?,
    val icon_Url: String,
    val power_levels: RoomPowerLevelsContent?
)

fun save_room_members(dir: String, users: List<UserState>) {
    val filename = dir + File.separator + usersfilename
    val file = File(filename)
    file.writeText("")
    val writer = file.bufferedWriter()
    for (user in users) {
        val u = user.id.toString()
        writer.append(u)
        writer.append('\n')
    }
    writer.close()
}
