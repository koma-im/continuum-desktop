package domain

import javafx.beans.property.SimpleListProperty
import javafx.collections.FXCollections
import koma.matrix.room.naming.RoomAlias
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
        val aliases: List<RoomAlias>?,
        val avatar_url: String?,
        val guest_can_join: Boolean,
        val name: String?,
        val num_joined_members: Int,
        val room_id: RoomId,
        val topic: String?,
        val world_readable: Boolean
) {
    fun dispName(): String{
        val n = name ?: aliases?.getOrNull(0)?.full ?: room_id.localstr
        return n
    }

    fun aliasesProperty(): SimpleListProperty<String> {
        val l = aliases?.map { it.full } ?: listOf()
        return SimpleListProperty(FXCollections.observableArrayList(l))
    }

    fun containsTerms(terms: List<String>): Boolean {
        fun String.containsAll(ss: List<String>): Boolean {
            return ss.all { this.contains(it, ignoreCase = true) }
        }
        if (this.aliases?.any {
                    // exclude the server name part, such as matrix.org
                    it.alias.containsAll(terms)
                } == true) return true
        if (this.name?.containsAll(terms) == true) return true
        if (this.topic?.containsAll(terms) == true) return true
        return false
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

