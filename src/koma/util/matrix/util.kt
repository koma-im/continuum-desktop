package koma.util.matrix

import koma.matrix.UserId
import koma.matrix.user.presence.PresenceMessage
import koma.model.user.UserState
import koma.storage.users.UserStore

fun UserId.getState() = UserStore.getOrCreateUserId(this)

fun PresenceMessage.getUserState(): UserState? {
    return UserStore.getOrCreateUserId(sender)
}
