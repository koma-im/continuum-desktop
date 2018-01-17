package koma.storage.config.profile

import com.squareup.moshi.Moshi
import koma.matrix.UserId
import koma.matrix.UserIdAdapter
import koma.storage.config.config_paths
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException

val filename = "last_used.json"

fun saveLastUsed(userId: UserId) {
    val dir = config_paths.profile_dir
    dir?: return
    val users = getRecentUsers().toMutableList()
    users.remove(userId)
    users.add(0, userId)
    val data = LastUsed(users.toList())
    val moshi = Moshi.Builder()
            .add(UserIdAdapter())
            .build()
    val jsonAdapter = moshi.adapter(LastUsed::class.java).indent("    ")
    val json = jsonAdapter.toJson(data)
    try {
        val file = File(dir).resolve(filename)
        file.writeText(json)
    } catch (e: IOException) {
    }
}

fun getRecentUsers(): List<UserId> {
    val dir = config_paths.profile_dir
    dir?:return listOf()
    val file = File(dir).resolve(filename)
    val jsonAdapter = Moshi.Builder()
            .add(UserIdAdapter())
            .build()
            .adapter(LastUsed::class.java)
    val lastUsed = try {
        jsonAdapter.fromJson(file.readText())
    } catch (e: FileNotFoundException) {
        println("$file not found")
        null
    } catch (e: IOException) {
        e.printStackTrace()
        null
    }
    return lastUsed?.last_used_users ?: listOf()
}

class LastUsed (
    val last_used_users: List<UserId>
)
