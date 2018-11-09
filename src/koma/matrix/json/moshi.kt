package koma.matrix.json

import com.squareup.moshi.KotlinJsonAdapterFactory
import com.squareup.moshi.Moshi
import koma.matrix.event.room_message.RoomEvent
import koma.matrix.event.room_message.chat.getPolyMessageAdapter
import koma.matrix.event.room_message.getPolyRoomEventAdapter

object MoshiInstance{
    val moshi = Moshi.Builder()
            .add(getPolyRoomEventAdapter())
            .add(getPolyMessageAdapter())
            .add(NewTypeStringAdapterFactory())
            .add(KotlinJsonAdapterFactory())
            .build()
    val roomEventAdapter = moshi.adapter(RoomEvent::class.java)
    init {
    }
}
