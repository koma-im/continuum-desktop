package koma.storage.persistence.account

import koma.Koma
import koma.matrix.UserId
import koma.matrix.json.MoshiInstance
import koma.storage.config.profile.userProfileDir
import matrix.AuthedUser
import mu.KotlinLogging
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException

private val logger = KotlinLogging.logger {}

private val tokenfilename = "access_token.json"

fun Koma.saveToken(userId: UserId, token: String) {
    val dir = userProfileDir(userId)
    dir?: return
    val data = Token(token)
    val moshi = MoshiInstance.moshi
    val jsonAdapter = moshi.adapter(Token::class.java).indent("    ")
    val json = jsonAdapter.toJson(data)
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

fun Koma.getToken(userId: UserId): AuthedUser? {
    val dir = userProfileDir(userId)
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
    val auth = token?.let { AuthedUser(it.token,user_id = userId ) }
    return auth
}
