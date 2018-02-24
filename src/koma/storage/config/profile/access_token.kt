package koma.storage.config.profile

import com.squareup.moshi.Moshi
import koma.matrix.UserId
import koma.matrix.json.NewTypeStringAdapterFactory
import matrix.AuthedUser
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException

private val tokenfilename = "access_token.json"

fun saveToken(userId: UserId, token: String) {
    val dir = userProfileDir(userId)
    dir?: return
    val data = Token(token)
    val moshi = Moshi.Builder()
            .add(NewTypeStringAdapterFactory())
            .build()
    val jsonAdapter = moshi.adapter(Token::class.java).indent("    ")
    val json = jsonAdapter.toJson(data)
    try {
        val file = File(dir).resolve(tokenfilename)
        file.writeText(json)
    } catch (e: IOException) {
    }
}

class Token (
    val token: String
)

fun getToken(userId: UserId): AuthedUser? {
    val dir = userProfileDir(userId)
    dir?: return null

    val file = File(dir).resolve(tokenfilename)
    val jsonAdapter = Moshi.Builder()
            .add(NewTypeStringAdapterFactory())
            .build()
            .adapter(Token::class.java)
    val token = try {
        jsonAdapter.fromJson(file.readText())
    } catch (e: FileNotFoundException) {
        println("$file not found")
        null
    } catch (e: IOException) {
        e.printStackTrace()
        null
    }
    val auth = token?.let { AuthedUser(it.token,user_id = userId ) }
    return auth
}
