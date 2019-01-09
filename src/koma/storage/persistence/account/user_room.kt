package koma.storage.persistence.account

import koma.Koma
import koma.koma_app.SaveToDiskTasks
import koma.koma_app.appState
import koma.matrix.UserId
import koma.matrix.json.MoshiInstance
import koma.storage.config.ConfigPaths
import koma.storage.config.profile.SavedUserState
import mu.KotlinLogging
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException

val userProfileFilename = "profile.json"

private val logger = KotlinLogging.logger {}

/**
 * loads token and joined rooms from disk
 * adds the rooms to a global object
 */
fun loadUser(koma: Koma, userId: UserId): Token?  {
    val paths = koma.paths
    val token = getToken(paths, userId)
    val s = loadUserState(paths, userId)
    val store = appState.accountRoomStore()
    if (s == null) {
        logger.warn { "no saved state is loaded from disk for user $userId" }
    } else if (store == null) {
        logger.warn { "Failed to get room store for user $userId" }
    } else {
        logger.debug { "$userId is known to be in ${s.joinedRooms.size} rooms" }
        for (r in s.joinedRooms) {
            store.add(r)
        }
    }
    SaveToDiskTasks.addJob {
        synchronized(koma) { saveProfile(paths, userId, token) }
    }
    return token
}

fun saveProfile(paths: ConfigPaths, userId: UserId, token: Token?) {
    val dir = paths.userProfileDir(userId)
    dir?: return
    if (token != null) saveToken(paths, userId, token)
    val rooms = appState.getAccountRoomStore(userId)!!.roomList
    val data = SavedUserState(
            joinedRooms = rooms.map { it.id }
    )
    val moshi = MoshiInstance.moshi
    val jsonAdapter = moshi.adapter(SavedUserState::class.java).indent("    ")
    val json = try {
        jsonAdapter.toJson(data)
    } catch (e: Exception) {
        logger.error { "failed to encode user $userId data $data: $e" }
        return
    }
    val file = File(dir).resolve(userProfileFilename)
    try {
        file.writeText(json)
    } catch (e: IOException) {
        logger.error { "failed to save user $userId data $data: $e" }
    }
}


fun loadUserState(paths: ConfigPaths, userId: UserId): SavedUserState? {
    val dir = paths.userProfileDir(userId)
    dir?:return null
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
