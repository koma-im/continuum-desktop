package koma.matrix.event.room_message

import com.squareup.moshi.JsonAdapter
import koma.matrix.event.room_message.state.*
import koma.matrix.json.RuntimeJsonAdapterFactory
import matrix.event.room_message.RoomEventType

fun getPolyRoomEventAdapter(): JsonAdapter.Factory {
    val factory = RuntimeJsonAdapterFactory<RoomEvent>(RoomEvent::class.java, "type", MRoomUnrecognized::class.java)
    factory.registerSubtype(MRoomAliases::class.java, RoomEventType.Aliases.toString())
    factory.registerSubtype(MRoomMessage::class.java, RoomEventType.Message.toString())
    factory.registerSubtype(MRoomAvatar::class.java, RoomEventType.Avatar .toString())
    factory.registerSubtype(MRoomCanonAlias::class.java, RoomEventType.CanonAlias .toString())
    factory.registerSubtype(MRoomCreate::class.java, RoomEventType.Create.toString())
    factory.registerSubtype(MRoomGuestAccess::class.java, RoomEventType.GuestAccess .toString())
    factory.registerSubtype(MRoomHistoryVisibility::class.java, RoomEventType.HistoryVisibility.toString())
    factory.registerSubtype(MRoomJoinRule::class.java, RoomEventType.JoinRule   .toString())
    factory.registerSubtype(MRoomMember::class.java, RoomEventType.Member.toString())
    factory.registerSubtype(MRoomName::class.java, RoomEventType.Name   .toString())
    factory.registerSubtype(MRoomPowerLevels::class.java, RoomEventType.PowerLevels.toString())
    factory.registerSubtype(MRoomRedaction::class.java, RoomEventType.Redaction   .toString())
    factory.registerSubtype(MRoomTopic::class.java, RoomEventType.Topic .toString())
    return factory
}
