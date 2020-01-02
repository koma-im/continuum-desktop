package link.continuum.desktop.database

import koma.matrix.room.naming.RoomId
import koma.storage.message.MessageManager
import java.util.concurrent.ConcurrentHashMap


class RoomMessages(private val data: KDataStore) {
    private val messages = ConcurrentHashMap<RoomId, MessageManager>()
    fun get(roomId: RoomId): MessageManager {
        return messages.computeIfAbsent(roomId) {
            MessageManager(roomId, data._dataStore)
        }
    }
}