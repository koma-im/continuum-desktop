package koma.matrix.event.rooRoomEvent

import com.squareup.moshi.*
import koma.matrix.event.room_message.*
import koma.matrix.event.room_message.state.*
import koma.matrix.json.RuntimeJsonAdapterFactory
import matrix.event.room_message.RoomEventType
import java.lang.reflect.Type

fun getPolyRoomEventAdapter(): JsonAdapter.Factory {
    val factory = RuntimeJsonAdapterFactory(RoomEvent::class.java, "type", MRoomUnrecognized::class.java)
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

class RoomEventAdapterFactory: JsonAdapter.Factory {
    override fun create(type: Type, annotations: MutableSet<out Annotation>, moshi: Moshi): JsonAdapter<RoomEvent>? {
        if (annotations.isNotEmpty() || Types.getRawType(type) != RoomEvent::class.java) {
            return null
        }
        return RoomEventAdapter(moshi)
    }
}

private class RoomEventAdapter(m: Moshi): JsonAdapter<RoomEvent>() {

    private val createSubAdapter: (Class<out RoomEvent>) -> JsonAdapter<RoomEvent> = {
        m.adapter<RoomEvent>(it)
    }
    private val keyToAdapters = mapOf<RoomEventType, JsonAdapter<RoomEvent>>(
            RoomEventType.Message to createSubAdapter(MRoomMessage::class.java),
            RoomEventType.Aliases to createSubAdapter(MRoomAliases::class.java),
            RoomEventType.CanonAlias to createSubAdapter(MRoomCanonAlias::class.java),
            RoomEventType.Create to createSubAdapter(MRoomCreate::class.java),
            RoomEventType.JoinRule to createSubAdapter(MRoomJoinRule::class.java),
            RoomEventType.PowerLevels to createSubAdapter(MRoomPowerLevels::class.java),
            RoomEventType.Member to createSubAdapter(MRoomMember::class.java),
            RoomEventType.Message to createSubAdapter(MRoomMessage::class.java),
            RoomEventType.Redaction to createSubAdapter(MRoomRedaction::class.java),
            RoomEventType.Name to createSubAdapter(MRoomName::class.java),
            RoomEventType.Topic to createSubAdapter(MRoomTopic::class.java),
            RoomEventType.Avatar to createSubAdapter(MRoomAvatar::class.java),
            RoomEventType.HistoryVisibility to createSubAdapter(MRoomHistoryVisibility::class.java),
            RoomEventType.GuestAccess to createSubAdapter(MRoomGuestAccess::class.java)
    )
    private val labels = keyToAdapters.keys.map { it.toString() }
    private val keyFinder = JsonKeyFinder("type")
    private val fallbackAdapter = m.adapter(MRoomUnrecognized::class.java)

    override fun toJson(writer: JsonWriter, msg: RoomEvent?) {
        msg ?: return
        val t = msg.type
        if (t != null) {
            val a: JsonAdapter<RoomEvent> = keyToAdapters[t] !!
            a.toJson(writer, msg)
        } else if (msg is MRoomUnrecognized) {
            fallbackAdapter.toJson(writer, msg)
        }
    }

    override fun fromJson(r: JsonReader): RoomEvent {
        val k = keyFinder.find(r.peekJson())
        val t = k?.let { RoomEventType.strToEnum(it)}
        val a = t?.let{ keyToAdapters[it] }
        val mm = a?.fromJson(r)
        val m = mm ?: fallbackAdapter.fromJson(r)!!
        return m
    }
}

