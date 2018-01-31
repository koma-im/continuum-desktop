package koma.matrix.event.room_message.state

import koma.matrix.UserId
import koma.matrix.event.room_message.RoomEvent
import koma.matrix.event.room_message.state.member.PrevContent
import koma.matrix.event.room_message.state.member.RoomMemberContent
import koma.matrix.event.room_message.state.member.StrippedState

sealed class RoomStateEvent(event_id: String, origin_server_ts: Long): RoomEvent(event_id, origin_server_ts)

class MRoomAliases(
        //val age: Long?,
        origin_server_ts: Long,
        event_id: String,
        val prev_content: Map<String, Any>?,
        val sender: UserId,
        val state_key: String,
        val content: RoomAliasesContent): RoomStateEvent(event_id, origin_server_ts) {
}
class MRoomCanonAlias(
        //val age: Long?,
        event_id: String,
        origin_server_ts: Long,
        val prev_content: Map<String, Any>?,
        val sender: UserId,
        val state_key: String?,
        val txn_id: String?,
        val content: RoomCanonAliasContent): RoomStateEvent(event_id, origin_server_ts)

class MRoomCreate(
        //val age: Long?,
        event_id: String,
        origin_server_ts: Long,
        val prev_content: Map<String, Any>?,
        val sender: UserId,
        val state_key: String?,
        val txn_id: String?,
        val content: RoomCreateContent): RoomStateEvent(event_id, origin_server_ts)

class MRoomJoinRule(
        //val age: Long?,
        event_id: String,
        origin_server_ts: Long,
        val prev_content: Map<String, Any>?,
        val sender: UserId,
        val state_key: String?,
        val txn_id: String?,
        val content: RoomJoinRulesContent): RoomStateEvent(event_id, origin_server_ts)

class MRoomMember(
        //val age: Long?,
        event_id: String,
        origin_server_ts: Long,
        val prev_content: PrevContent?,
        val sender: UserId,
        val state_key: String?,
        val invite_room_state: List<StrippedState>?,
        val content: RoomMemberContent): RoomStateEvent(event_id, origin_server_ts)

class MRoomPowerLevels(
        //val age: Long?,
        event_id: String,
        origin_server_ts: Long,
        val prev_content: Map<String, Any>?,
        val sender: UserId,
        val state_key: String?,
        val txn_id: String?,
        val content: RoomPowerLevelsContent): RoomStateEvent(event_id, origin_server_ts)

class MRoomPinnedEvents(
        //val age: Long?,
        event_id: String,
        origin_server_ts: Long,
        val prev_content: Map<String, Any>?,
        val sender: UserId,
        val state_key: String?,
        val txn_id: String?,
        val content: RoomPinnedEventsContent): RoomStateEvent(event_id, origin_server_ts)


class MRoomTopic(
        //val age: Long?,
        event_id: String,
        origin_server_ts: Long,
        val prev_content: Map<String, Any>?,
        val sender: UserId,
        val state_key: String?,
        val txn_id: String?,
        val content: RoomTopicContent): RoomStateEvent(event_id, origin_server_ts)

class MRoomName(
        //val age: Long?,
        event_id: String,
        origin_server_ts: Long,
        val prev_content: Map<String, Any>?,
        val sender: UserId,
        val state_key: String?,
        val txn_id: String?,
        val content: RoomNameContent): RoomStateEvent(event_id, origin_server_ts)

class MRoomAvatar(
        //val age: Long?,
        event_id: String,
        origin_server_ts: Long,
        val prev_content: Map<String, Any>?,
        val sender: UserId,
        val state_key: String?,
        val txn_id: String?,
        val content: RoomAvatarContent): RoomStateEvent(event_id, origin_server_ts)

class MRoomHistoryVisibility(
        //val age: Long?,
        event_id: String,
        origin_server_ts: Long,
        val prev_content: Map<String, Any>?,
        val sender: UserId,
        val state_key: String?,
        val txn_id: String?,
        val content: RoomHistoryVisibilityContent): RoomStateEvent(event_id, origin_server_ts)

class MRoomGuestAccess(
        //val age: Long?,
        event_id: String,
        origin_server_ts: Long,
        val prev_content: Map<String, Any>?,
        val sender: UserId,
        val state_key: String?,
        val txn_id: String?,
        val content: Map<String, Any>): RoomStateEvent(event_id, origin_server_ts)

