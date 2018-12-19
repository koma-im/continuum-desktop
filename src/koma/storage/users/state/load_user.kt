package koma.storage.users.state

import com.squareup.moshi.Moshi
import koma.matrix.UserId
import koma.model.user.UserState
import koma.storage.config.ConfigPaths
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException

fun load_user(userId: UserId, paths: ConfigPaths): UserState {
    val us = UserState(userId)
    val dir = paths.user_save_path(userId)
    dir?: return us
    val sf = File(dir).resolve(userId.user+".json")
    val jsonAdapter = Moshi.Builder()
            .build()
            .adapter(SavedUserState::class.java)
    val savedUser = try {
        jsonAdapter.fromJson(sf.readText())
    } catch (e: FileNotFoundException) {
        null
    } catch (e: IOException) {
        e.printStackTrace()
        null
    }
    savedUser?: return us
    us.avatar = savedUser.avatarUrl
    us.name = savedUser.name
    us.modified = false

    return us
}
