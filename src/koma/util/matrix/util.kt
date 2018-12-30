package koma.util.matrix

import koma.matrix.UserId
import koma.matrix.user.presence.PresenceMessage
import koma.model.user.UserState
import koma.koma_app.appState

fun UserId.getState() = appState.userStore.getOrCreateUserId(this)

fun PresenceMessage.getUserState(): UserState? {
    return appState.userStore.getOrCreateUserId(sender)
}
