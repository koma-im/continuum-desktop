package koma.storage.config.profile

import koma.Koma
import koma.koma_app.SaveJobs
import koma.matrix.UserId
import koma.matrix.json.MoshiInstance
import koma.storage.persistence.account.getToken
import koma.storage.persistence.account.saveToken
import koma_app.appState
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException

val userProfileFilename = "profile.json"



fun Koma.newProfile(userId: UserId): Profile?  {
    val token = getToken(userId)
    token?: return null
    val profile = Profile(userId, token.access_token)
    val s = loadUserState(userId)
    val store = appState.accountRoomStore()
    if (s!= null && store != null) {
        for (r in s.joinedRooms) {
            store.add(r)

        }
    }
    SaveJobs.addJob {
        synchronized(this) {this.saveProfile(profile) }
    }
    return profile
}


fun Koma.saveProfile(profile: Profile) {
    val userId = profile.userId
    val access_token = profile.access_token
    val dir = userProfileDir(userId)
    dir?: return
    saveToken(userId, access_token)
    val rooms = appState.getAccountRoomStore(profile.userId)!!.roomList
    val data = SavedUserState(
            joinedRooms = rooms.map { it.id }
    )
    val moshi = MoshiInstance.moshi
    val jsonAdapter = moshi.adapter(SavedUserState::class.java).indent("    ")
    val json = try {
        jsonAdapter.toJson(data)
    } catch (e: ClassCastException) {
        e.printStackTrace()
        return
    }
    val file = File(dir).resolve(userProfileFilename)
    try {
        file.writeText(json)
    } catch (e: IOException) {
    }
}


fun Koma.loadUserState(userId: UserId): SavedUserState? {
    val dir = userProfileDir(userId)
    dir?:return null
    val file = File(dir).resolve(userProfileFilename)
    val jsonAdapter = MoshiInstance.moshi.adapter(SavedUserState::class.java)
    val savedRoomState = try {
        jsonAdapter.fromJson(file.readText())
    } catch (e: FileNotFoundException) {
        println("$file not found")
        return null
    } catch (e: IOException) {
        e.printStackTrace()
        return null
    }
    return savedRoomState
}
