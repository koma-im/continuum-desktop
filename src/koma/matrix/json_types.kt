package domain

import javafx.beans.property.SimpleListProperty
import javafx.collections.FXCollections
import koma.matrix.user.presence.PresenceMessage
import model.RoomInitialSyncResult

/**
 * Created by developer on 2017/7/8.
 * json type of classes
 */
data class Chunked<T>(
        val start: String?,
        val end: String?,
        // can be null when the transferring is done
        val chunk: List<T>
)

data class DiscoveredRoom(
        val aliases: List<String>,
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
        else if (aliases.size >0 )
            aliases[0]
        else
            room_id
        return dispname
    }

    fun remainingAliasesProp(): SimpleListProperty<String> {
        val rem_aliases = if (name != null)
            aliases
        else {
            aliases.drop(1)
        }
        return SimpleListProperty( FXCollections.observableArrayList(rem_aliases))
    }

    fun aliasesProperty(): SimpleListProperty<String> {
        return SimpleListProperty(FXCollections.observableArrayList(aliases))
    }
}


data class AvatarUrl(
        val avatar_url: String
)

data class UploadResponse(
        val content_uri: String
)

data class RoomInfo(
        val room_id: String)

class EmptyResult()


/**
 * Created by developer on 2017/7/5.
 */

data class ClientInitialSyncResult(
        val end: String,
        val presence: List<PresenceMessage>,
        val rooms: List<RoomInitialSyncResult>
)
