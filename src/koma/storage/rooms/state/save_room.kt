package koma.storage.rooms.state

import com.squareup.moshi.Moshi
import koma.matrix.event.room_message.state.RoomPowerLevelsContent
import koma.matrix.json.NewTypeStringAdapterFactory
import koma.matrix.room.naming.RoomAlias
import koma.matrix.room.participation.RoomJoinRules
import koma.matrix.room.visibility.HistoryVisibility
import koma.matrix.room.visibility.RoomVisibility
import koma.model.user.UserState
import koma.storage.config.config_paths
import model.Room
import java.io.File
import java.io.IOException

val statefilename = "room_state.json"
val usersfilename = "users.txt"

fun state_save_path(vararg paths: String): String? {
    return config_paths.getOrCreate("state", *paths)
}

fun Room.save() {
    synchronized(this) { this.saveUnsync() }
}

private fun Room.saveUnsync(){
    val dir = state_save_path(
            this.id.servername,
            this.id.localstr)
    dir?: return
    val data = SavedRoomState(
            this.aliases,
            this.visibility,
            this.joinRule,
            this.histVisibility,
            this.name.get(),
            this.iconURL,
            this.power_levels
    )
    val moshi = Moshi.Builder()
            .add(NewTypeStringAdapterFactory())
            .build()
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
    save_room_members(dir, this.members.sorted())
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
