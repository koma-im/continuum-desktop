package koma.matrix.json

import com.squareup.moshi.Moshi
import koma.matrix.event.rooRoomEvent.RoomEventAdapterFactory
import koma.matrix.event.room_message.RoomEvent
import koma.matrix.event.room_message.chat.MessageAdapterFactory

object MoshiInstance{
    val moshi = Moshi.Builder()
            .add(MessageAdapterFactory())
            .add(RoomEventAdapterFactory())
            .add(NewTypeStringAdapterFactory())
            .build()
    val roomEventAdapter = moshi.adapter(RoomEvent::class.java)
    init {
    }
}
