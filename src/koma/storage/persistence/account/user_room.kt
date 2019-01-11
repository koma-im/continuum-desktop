package koma.storage.persistence.account

import koma.koma_app.SaveToDiskTasks
import koma.matrix.UserId
import koma.matrix.json.MoshiInstance
import koma.storage.config.ConfigPaths
import koma.storage.config.profile.SavedUserState
import koma.storage.rooms.UserRoomStore
import mu.KotlinLogging
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException

val userProfileFilename = "profile.json"

private val logger = KotlinLogging.logger {}

/**
 * loads joined rooms from disk
 * adds the rooms to a global object
 */
fun loadJoinedRooms(
        paths: ConfigPaths,
        store: UserRoomStore,
        userId: UserId
) {
    val dir = paths.userProfileDir(userId)
    dir ?: return
    SaveToDiskTasks.addJob {
        saveJoinedRooms(dir, store)
    }

    val s = loadUserState(dir, userId)
    if (s != null) {
        logger.debug { "$userId is known to be in ${s.joinedRooms.size} rooms" }
        for (r in s.joinedRooms) {
            store.add(r)
        }
    } else {
        logger.warn { "no saved state is loaded from disk for user $userId" }
    }
}

/**
 * save joined rooms to disk
 */
private fun saveJoinedRooms(dir: String, user: UserRoomStore) {
    val rooms = user.roomList
    val data = SavedUserState(
            joinedRooms = rooms.map { it.id }
    )
    val moshi = MoshiInstance.moshi
    val jsonAdapter = moshi.adapter(SavedUserState::class.java).indent("    ")
    val json = try {
        jsonAdapter.toJson(data)
    } catch (e: Exception) {
        logger.error { "failed to encode user data $data: $e" }
        return
    }
    val file = File(dir).resolve(userProfileFilename)
    try {
        file.writeText(json)
    } catch (e: IOException) {
        logger.error { "failed to save user data $data: $e" }
    }
}

/**
 * load joined rooms
 */
private fun loadUserState(dir: String, userId: UserId): SavedUserState? {
    val file = File(dir).resolve(userProfileFilename)
    val jsonAdapter = MoshiInstance.moshi.adapter(SavedUserState::class.java)
    val savedRoomState = try {
        jsonAdapter.fromJson(file.readText())
    } catch (e: FileNotFoundException) {
        logger.warn { "User $userId has no saved state at $file" }
        return null
    } catch (e: IOException) {
        logger.warn { "User $userId's saved state $file failed to load: $e" }
        return null
    }
    return savedRoomState
}
