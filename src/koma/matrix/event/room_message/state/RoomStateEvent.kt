package koma.matrix.event.room_message.state

import koma.matrix.UserId
import koma.matrix.event.EventId
import koma.matrix.event.room_message.RoomEvent
import koma.matrix.event.room_message.state.member.PrevContent
import koma.matrix.event.room_message.state.member.RoomMemberContent
import koma.matrix.event.room_message.state.member.RoomMemberUnsigned
import koma.matrix.event.room_message.state.member.StrippedState
import matrix.event.room_message.RoomEventType

sealed class RoomStateEvent(
        event_id: EventId,
        origin_server_ts: Long,
        type: RoomEventType)
    : RoomEvent(event_id, origin_server_ts, type)

class MRoomAliases(
        //val age: Long?,
        origin_server_ts: Long,
        event_id: EventId,
        val prev_content: Map<String, Any>?,
        val sender: UserId,
        val state_key: String,
        val content: RoomAliasesContent
): RoomStateEvent(event_id, origin_server_ts, RoomEventType.Aliases)

class MRoomCanonAlias(
        //val age: Long?,
        event_id: EventId,
        origin_server_ts: Long,
        val prev_content: Map<String, Any>?,
        val sender: UserId,
        val state_key: String?,
        val txn_id: String?,
        val content: RoomCanonAliasContent
): RoomStateEvent(event_id, origin_server_ts, RoomEventType.CanonAlias)

class MRoomCreate(
        //val age: Long?,
        event_id: EventId,
        origin_server_ts: Long,
        val prev_content: Map<String, Any>?,
        val sender: UserId,
        val state_key: String?,
        val txn_id: String?,
        val content: RoomCreateContent
): RoomStateEvent(event_id, origin_server_ts, RoomEventType.Create)

class MRoomJoinRule(
        //val age: Long?,
        event_id: EventId,
        origin_server_ts: Long,
        val prev_content: Map<String, Any>?,
        val sender: UserId,
        val state_key: String?,
        val txn_id: String?,
        val content: RoomJoinRulesContent
): RoomStateEvent(event_id, origin_server_ts, RoomEventType.JoinRule)

class MRoomMember(
        //val age: Long?,
        event_id: EventId,
        origin_server_ts: Long,
        val prev_content: PrevContent?,
        val sender: UserId,
        val unsigned: RoomMemberUnsigned?,
        val replaces_state: String?,
        val state_key: String?,
        val invite_room_state: List<StrippedState>?,
        val content: RoomMemberContent
): RoomStateEvent(event_id, origin_server_ts, RoomEventType.Member)

class MRoomPowerLevels(
        //val age: Long?,
        event_id: EventId,
        origin_server_ts: Long,
        val prev_content: Map<String, Any>?,
        val sender: UserId,
        val state_key: String?,
        val content: RoomPowerLevelsContent
): RoomStateEvent(event_id, origin_server_ts, RoomEventType.PowerLevels)

class MRoomPinnedEvents(
        //val age: Long?,
        event_id: EventId,
        origin_server_ts: Long,
        val prev_content: Map<String, Any>?,
        val sender: UserId,
        val state_key: String?,
        val txn_id: String?,
        val content: RoomPinnedEventsContent
): RoomStateEvent(event_id, origin_server_ts, RoomEventType.PinnedEvents)


class MRoomTopic(
        //val age: Long?,
        event_id: EventId,
        origin_server_ts: Long,
        val prev_content: Map<String, Any>?,
        val sender: UserId,
        val state_key: String?,
        val txn_id: String?,
        val content: RoomTopicContent
): RoomStateEvent(event_id, origin_server_ts, RoomEventType.Topic)

class MRoomName(
        //val age: Long?,
        event_id: EventId,
        origin_server_ts: Long,
        val prev_content: Map<String, Any>?,
        val sender: UserId,
        val state_key: String?,
        val txn_id: String?,
        val content: RoomNameContent
): RoomStateEvent(event_id, origin_server_ts, RoomEventType.Name)

class MRoomAvatar(
        //val age: Long?,
        event_id: EventId,
        origin_server_ts: Long,
        val prev_content: Map<String, Any>?,
        val sender: UserId,
        val state_key: String?,
        val txn_id: String?,
        val content: RoomAvatarContent
): RoomStateEvent(event_id, origin_server_ts, RoomEventType.Avatar)

class MRoomHistoryVisibility(
        //val age: Long?,
        event_id: EventId,
        origin_server_ts: Long,
        val prev_content: Map<String, Any>?,
        val sender: UserId,
        val state_key: String?,
        val txn_id: String?,
        val content: RoomHistoryVisibilityContent
): RoomStateEvent(event_id, origin_server_ts, RoomEventType.HistoryVisibility)

class MRoomGuestAccess(
        //val age: Long?,
        event_id: EventId,
        origin_server_ts: Long,
        val prev_content: Map<String, Any>?,
        val sender: UserId,
        val state_key: String?,
        val txn_id: String?,
        val content: Map<String, Any>
): RoomStateEvent(event_id, origin_server_ts, RoomEventType.GuestAccess)

