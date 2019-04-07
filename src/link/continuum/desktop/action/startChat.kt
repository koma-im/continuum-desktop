package link.continuum.desktop.action

import koma.Koma
import koma.gui.view.ChatWindowBars
import koma.gui.view.SyncStatusBar
import koma.koma_app.appState
import koma.matrix.UserId
import link.continuum.desktop.database.KDataStore
import link.continuum.desktop.database.models.updateAccountUsage
import mu.KotlinLogging
import okhttp3.HttpUrl
import tornadofx.*

private val logger = KotlinLogging.logger {}

/**
 * show the chat window after login is done
 * updates the list of recently used accounts
 */
fun startChat(koma: Koma, userId: UserId, token: String, url: HttpUrl, data: KDataStore) {
    updateAccountUsage(data, userId)

    val app = appState
    val store = app.store
    val apiClient  = koma.createApi(token, userId, url)
    app.currentUser = userId
    app.apiClient = apiClient
    val userRooms = store.getAccountRoomStore(userId)

    val primary = ChatWindowBars(userRooms.roomList, url, data, store, koma.http.client)
    val statusBar = SyncStatusBar()
    primary.statusBar.add(statusBar.root)
    FX.primaryStage.scene.root = primary.root

    val fullSync = userRooms.roomList.isEmpty()
    if (fullSync) logger.warn { "Doing a full sync because there " +
            "are no known rooms $userId has joined" }
    val sync = SyncControl(
            apiClient,
            userId,
            statusChan = statusBar.status,
            full_sync =  fullSync,
            data = data,
            userDataStore = store.userData
    )

    sync.start()
}
