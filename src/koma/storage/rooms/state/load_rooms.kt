package koma.storage.rooms.state

import com.squareup.moshi.JsonDataException
import koma.koma_app.appState
import koma.matrix.json.MoshiInstance
import koma.matrix.room.naming.RoomId
import koma.matrix.room.visibility.HistoryVisibilityCaseInsensitiveAdapter
import koma.matrix.room.visibility.RoomVisibilityCaseInsensitiveAdapter
import koma.matrix.user.identity.UserId_new
import koma.storage.config.ConfigPaths
import model.Room
import mu.KotlinLogging
import okhttp3.HttpUrl
import java.io.File
import java.io.IOException
import java.util.stream.Stream

private val logger = KotlinLogging.logger {}



fun ConfigPaths.loadRoom(roomId: RoomId): Room? {
    logger.debug { "Loading room with id $roomId" }
    val state_dir = this.getCreateDir("state")
    state_dir?: return null
    val dir = state_dir.resolve(roomId.servername).resolve(roomId.localstr)
    if (!dir.isDirectory) return null
    return loadRoomAt(roomId, dir)
}

private fun loadRoomAt(roomId: RoomId, roomDir: File): Room? {
    val sf = File(roomDir.resolve(statefilename).absolutePath)
    val jsonAdapter = MoshiInstance.moshiBuilder
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
    room.name.set(savedRoomState.name)
    room.iconURL= HttpUrl.parse(savedRoomState.icon_Url)
    room.histVisibility = savedRoomState.history_visibility
    room.joinRule = savedRoomState.join_rule
    room.visibility = savedRoomState.visibility
    room.power_levels = savedRoomState.power_levels

    val members = load_members(roomDir.resolve(usersfilename))
    for (m in members) {
        val u = UserId_new(m.first)
        room.members.add(appState.store.userStore.getOrCreateUserId(u))
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
