package koma.storage.persistence.account

import koma.matrix.UserId
import koma.matrix.json.MoshiInstance
import koma.storage.config.ConfigPaths
import mu.KotlinLogging
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException

private val logger = KotlinLogging.logger {}

private val tokenfilename = "access_token.json"

fun saveToken(paths: ConfigPaths, userId: UserId, token: Token?) {
    token ?: return
    val dir = paths.userProfileDir(userId)
    dir?: return
    val moshi = MoshiInstance.moshi
    val jsonAdapter = moshi.adapter(Token::class.java).indent("    ")
    val json = jsonAdapter.toJson(token)
    try {
        val file = File(dir).resolve(tokenfilename)
        file.writeText(json)
    } catch (e: IOException) {
        logger.warn { "Failed to save token of $userId: $e" }
    }
}

class Token (
        val token: String
)

/**
 * loads access token from disk
 */
fun getToken(paths: ConfigPaths, userId: UserId): Token? {
    val dir = paths.userProfileDir(userId)
    dir?: return null

    val file = File(dir).resolve(tokenfilename)
    val jsonAdapter = MoshiInstance.moshi
            .adapter(Token::class.java)
    val token = try {
        jsonAdapter.fromJson(file.readText())
    } catch (e: FileNotFoundException) {
        logger.warn { "Failed to find token file of $userId: $e" }
        null
    } catch (e: IOException) {
        logger.warn { "Failed to load token of $userId: $e" }
        null
    }
    return token
}
