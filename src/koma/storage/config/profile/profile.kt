package koma.storage.config.profile

import com.squareup.moshi.Moshi
import javafx.collections.ObservableList
import koma.matrix.UserId
import koma.matrix.room.naming.RoomId
import koma.matrix.room.naming.RoomIdAdapter
import koma.storage.rooms.UserRoomStore
import model.Room
import util.getToken
import util.saveToken
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException

val userProfileFilename = "profile.json"

class Profile(
        val userId: UserId,
        val access_token: String
) {

    val roomStore = UserRoomStore()

    val hasRooms: Boolean

    fun getRoomList(): ObservableList<Room> = roomStore.roomList

    init {
        val s = loadUserState(userId)
        if (s!= null && s.joinedRooms.isNotEmpty()) {
            hasRooms = true
            for (r in s.joinedRooms) {
                roomStore.add(r)
            }
        } else {
            hasRooms = false
        }

        Runtime.getRuntime().addShutdownHook(Thread({
            this.save()
        }))
    }

    companion object {
        fun new(userId: UserId): Profile?{
            val token = userProfileDir(userId)?.let {  getToken(userId, it)}
            return token?.access_token?.let { Profile(userId, it) }
        }
    }
}

class SavedUserState (
    val joinedRooms: List<RoomId>
)

private fun loadUserState(userId: UserId): SavedUserState? {
    val dir = userProfileDir(userId)
    dir?:return null
    val file = File(dir).resolve(userProfileFilename)
    val jsonAdapter = Moshi.Builder()
            .add(RoomIdAdapter())
            .build()
            .adapter(SavedUserState::class.java)
    val savedRoomState = try {
        jsonAdapter.fromJson(file.readText())
    } catch (e: FileNotFoundException) {
        println("$file not found")
        return null
    } catch (e: IOException) {
        e.printStackTrace()
        return null
    }
    return savedRoomState
}

fun Profile.save() {
    val dir = userProfileDir(userId)
    dir?: return
    saveToken(userId, access_token, dir)
    val data = SavedUserState(
            joinedRooms = getRoomList().map { it.id }
    )
    val moshi = Moshi.Builder()
            .add(RoomIdAdapter())
            .build()
    val jsonAdapter = moshi.adapter(SavedUserState::class.java).indent("    ")
    val json = try {
        jsonAdapter.toJson(data)
    } catch (e: ClassCastException) {
        e.printStackTrace()
        return
    }
    val file = File(dir).resolve(userProfileFilename)
    try {
        file.writeText(json)
    } catch (e: IOException) {
    }
}
