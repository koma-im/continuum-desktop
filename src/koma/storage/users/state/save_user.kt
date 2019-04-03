package koma.storage.users.state

import com.squareup.moshi.Moshi
import koma.matrix.UserId
import koma.model.user.UserState
import koma.storage.config.ConfigPaths
import java.io.File
import java.io.IOException

fun ConfigPaths.user_save_path(userId: UserId): String? {
    return this.getOrCreate("people", userId.server)
}

fun ConfigPaths.saveUser(user: UserState) {
    if (!user.modified) return
    val dir = user_save_path(user.id)
    dir?: return
    val data = SavedUserState(
            user.name,
            user.avatar.toString()
    )
    val moshi = Moshi.Builder()
            .build()
    val jsonAdapter = moshi.adapter(SavedUserState::class.java).indent("    ")
    val json = try {
        jsonAdapter.toJson(data)
    } catch (e: ClassCastException) {
        e.printStackTrace()
        return
    }
    try {
        val file = File(dir).resolve(user.id.user+".json")
        file.writeText(json)
    } catch (e: IOException) {
    }
}

class SavedUserState (
    val name: String,
    val avatarUrl: String
)

