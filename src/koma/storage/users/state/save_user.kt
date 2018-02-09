package koma.storage.users.state

import com.squareup.moshi.Moshi
import koma.matrix.UserId
import koma.model.user.UserState
import koma.storage.config.config_paths
import java.io.File
import java.io.IOException

fun user_save_path(userId: UserId): String? {
    return config_paths.getOrCreate("people", userId.server)
}

fun UserState.save() {
    synchronized(this) { this.saveUnsync() }
}

fun UserState.saveUnsync() {
    if (!this.modified) return
    val dir = user_save_path(id)
    dir?: return
    val data = SavedUserState(
            this.name,
            this.avatar
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
        val file = File(dir).resolve(id.user+".json")
        file.writeText(json)
    } catch (e: IOException) {
    }
}

class SavedUserState (
    val name: String,
    val avatarUrl: String
)

