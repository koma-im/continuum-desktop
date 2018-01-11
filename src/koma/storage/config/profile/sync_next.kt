package koma.storage.config.profile

import koma.matrix.UserId
import koma.storage.config.config_paths
import java.io.File
import java.io.IOException

fun userProfileDir(userid: UserId): String? {
    return config_paths.getCreateProfileDir(userid.server, userid.user)
}

fun saveSyncBatchToken(userid: UserId, next_batch: String) {
    val userdir = userProfileDir(userid)
    userdir?: return
    val syncTokenFile = File(userdir).resolve("next_batch")
    try {
        syncTokenFile.writeText(next_batch)
    } catch (e: IOException) {
        return
    }
}

fun loadSyncBatchToken(userid: UserId): String? {
    val userdir = userProfileDir(userid)
    userdir?: return null
    val syncTokenFile = File(userdir).resolve("next_batch")
    try {
        val batch = syncTokenFile.readText()
        syncTokenFile.delete()
        return batch
    } catch (e: IOException) {
        return null
    }
}
