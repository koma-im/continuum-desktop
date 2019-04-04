package koma.util.matrix

import koma.koma_app.appState
import koma.matrix.UserId
import koma.matrix.user.presence.PresenceMessage
import koma.model.user.UserState

fun UserId.getState() = appState.store.userStore.getOrCreateUserId(this)

fun PresenceMessage.getUserState(): UserState? {
    return appState.store.userStore.getOrCreateUserId(sender)
}
