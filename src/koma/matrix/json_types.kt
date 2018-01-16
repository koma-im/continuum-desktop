package domain

import javafx.beans.property.SimpleListProperty
import javafx.collections.FXCollections
import koma.matrix.room.naming.RoomId

/**
 * Created by developer on 2017/7/8.
 * json type of classes
 */
data class Chunked<T>(
        val start: String?,
        val end: String,
        // can be null when the transferring is done
        val chunk: List<T>
)

data class DiscoveredRoom(
        val aliases: List<String>?,
        val avatar_url: String?,
        val guest_can_join: Boolean,
        val name: String?,
        val num_joined_members: Int,
        val room_id: String,
        val topic: String?,
        val world_readable: Boolean
) {
    fun dispName(): String {
        val dispname = if (name != null)
            name
        else {
            aliases?.getOrNull(0)?:room_id
        }
        return dispname
    }

    fun aliasesProperty(): SimpleListProperty<String> {
        val l = aliases ?: listOf()
        return SimpleListProperty(FXCollections.observableArrayList(l))
    }
}


data class AvatarUrl(
        val avatar_url: String
)

data class UploadResponse(
        val content_uri: String
)

data class RoomInfo(
        val room_id: RoomId)

class EmptyResult()

