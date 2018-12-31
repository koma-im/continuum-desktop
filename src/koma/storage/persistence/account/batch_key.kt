package koma.storage.persistence.account

import koma.matrix.UserId
import koma.storage.config.ConfigPaths
import mu.KotlinLogging
import java.io.File
import java.io.IOException


private val logger = KotlinLogging.logger {}

fun ConfigPaths.userProfileDir(userid: UserId): String? {
    return this.getCreateProfileDir(userid.server, userid.user)
}

fun ConfigPaths.saveSyncBatchToken(userid: UserId, next_batch: String) {
    val userdir = userProfileDir(userid)
    userdir?: return
    val syncTokenFile = File(userdir).resolve("next_batch")
    try {
        syncTokenFile.writeText(next_batch)
    } catch (e: IOException) {
        logger.warn {
            "Failed to save sync pagination to ${syncTokenFile.path}: $e"
        }
        return
    }
}

fun ConfigPaths.loadSyncBatchToken(userid: UserId): String? {
    val userdir = userProfileDir(userid)
    if (userdir == null) {
        logger.warn {
            "No sync pagination stored yet for user $userid"
        }
        return null
    }
    val syncTokenFile = File(userdir).resolve("next_batch")
    try {
        val batch = syncTokenFile.readText()
        syncTokenFile.delete()
        return batch
    } catch (e: IOException) {
        logger.warn {
            "Failed to load sync pagination from ${syncTokenFile.path}: $e"
        }
        return null
    }
}
