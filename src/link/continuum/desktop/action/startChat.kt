package link.continuum.desktop.action

import koma.Koma
import koma.gui.view.ChatWindowBars
import koma.gui.view.SyncStatusBar
import koma.koma_app.appState
import koma.matrix.UserId
import koma.storage.config.profile.saveLastUsed
import koma.storage.persistence.account.loadJoinedRooms
import link.continuum.desktop.database.KDataStore
import mu.KotlinLogging
import okhttp3.HttpUrl
import tornadofx.*

private val logger = KotlinLogging.logger {}

/**
 * show the chat window after login is done
 * updates the list of recently used accounts
 */
fun startChat(koma: Koma, userId: UserId, token: String, url: HttpUrl, data: KDataStore) {
    koma.saveLastUsed(userId)

    val app = appState
    val apiClient  = koma.createApi(token, userId, url)
    app.currentUser = userId
    app.apiClient = apiClient

    val userRooms = app.getAccountRoomStore(userId)!!
    loadJoinedRooms(koma.paths, userRooms, userId)

    val primary = ChatWindowBars(userRooms.roomList, url)
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
            data = data
    )

    sync.start()
}
