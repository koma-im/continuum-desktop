package koma.storage.rooms.state

import com.squareup.moshi.JsonDataException
import com.squareup.moshi.Moshi
import koma.matrix.room.naming.RoomAliasAdapter
import koma.matrix.room.naming.RoomId
import koma.matrix.room.naming.RoomIdAdapter
import koma.matrix.room.visibility.HistoryVisibilityCaseInsensitiveAdapter
import koma.matrix.room.visibility.RoomVisibilityCaseInsensitiveAdapter
import koma.matrix.user.identity.UserId_new
import koma.storage.config.config_paths
import koma.storage.users.UserStore
import model.Room
import java.io.File
import java.io.IOException
import java.util.stream.Stream

val state_dir = config_paths.getOrCreate("state")

fun load_rooms(): Iterable<Room> {
    val f = File(state_dir)
    val rooms = mutableListOf<Room>()
    for (s in f.list()) {
        val serv = f.resolve(s)
        val rs = serv.list()
        if (rs != null) {
            for (r in rs) {
                val roomid = RoomId(s, r)
                val path = serv.resolve(r)
                load_room(roomid, path)?.let { rooms.add(it) }
            }
        }
    }
    return rooms
}

fun load_room(roomId: RoomId, path: File): Room? {
    val sf = File(path.resolve(statefilename).absolutePath)
    val jsonAdapter = Moshi.Builder()
            .add(RoomIdAdapter())
            .add(RoomAliasAdapter())
            .add(HistoryVisibilityCaseInsensitiveAdapter())
            .add(RoomVisibilityCaseInsensitiveAdapter())
            .build()
            .adapter(SavedRoomState::class.java)
    val savedRoomState = try {
        jsonAdapter.fromJson(sf.readText())
    } catch (e: IOException) {
        e.printStackTrace()
        return null
    } catch (de: JsonDataException) {
        de.printStackTrace()
        return null
    }
    savedRoomState?: return null
    val room = Room(roomId)
    room.aliases.setAll(savedRoomState.aliases)
    room.displayName.set(savedRoomState.name)
    room.iconURL=savedRoomState.icon_Url
    room.histVisibility = savedRoomState.history_visibility
    room.joinRule = savedRoomState.join_rule
    room.visibility = savedRoomState.visibility
    room.power_levels.putAll(savedRoomState.power_levels)

    val members = load_members(path.resolve(usersfilename))
    for (m in members) {
        val u = UserId_new(m.first)
        room.members.add(UserStore.getOrCreateUserId(u))
        val l = m.second
        if (l != null) {
            room.userStates.get(u).power = l
        }
    }
    return room
}

fun load_members(file: File): Stream<Pair<String, Float?>>
        = file.bufferedReader().lines().map {
        val l = it.split(' ', limit = 2)
        val user = l[0]
        val lvl = l.getOrNull(1)?.toFloatOrNull()
        user to lvl
    }
