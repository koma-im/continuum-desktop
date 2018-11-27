package koma.matrix.event.room_message.chat

import com.squareup.moshi.*
import java.lang.reflect.Type

class MessageAdapterFactory: JsonAdapter.Factory {
    override fun create(type: Type, annotations: MutableSet<out Annotation>, moshi: Moshi): JsonAdapter<M_Message>? {
        if (annotations.isNotEmpty() || Types.getRawType(type) != M_Message::class.java) {
            return null
        }
        return MessageAdapter(moshi)
    }
}

private class MessageAdapter(m: Moshi): JsonAdapter<M_Message>() {
    private val labelKeyOption = JsonReader.Options.of("msgtype")
    private val makeMA: (Class<out M_Message>)-> JsonAdapter<M_Message> = {
        m.adapter<M_Message>(it)
    }
    private val keyToAdapters = mapOf<String, JsonAdapter<M_Message>>(
            "m.text" to m.adapter<M_Message>(  TextMessage::class.java),
            "m.emote" to makeMA(       EmoteMessage::class.java),
            "m.notice" to makeMA(     NoticeMessage::class.java),
            "m.image" to makeMA(       ImageMessage::class.java),
            "m.file" to makeMA( FileMessage::class.java),
            "m.location" to makeMA(  LocationMessage::class.java),
            "m.video" to makeMA(       VideoMessage::class.java),
            "m.audio"  to makeMA(     AudioMessage::class.java)
    )
    private val labels = keyToAdapters.keys.toList()
    private val labelOptions = JsonReader.Options.of(*labels.toTypedArray())

    override fun toJson(writer: JsonWriter, msg: M_Message?) {
        msg ?: return
        val t: String? = msg.getMsgType()
        val a: JsonAdapter<M_Message>? = t?.let { keyToAdapters[it] }
        a?.toJson(writer, msg)
    }

    override fun fromJson(r: JsonReader): M_Message {
        val t = findType(r.peekJson())
        val a = t?.let { keyToAdapters[it] }
        val m = a?.fromJson(r)
        val raw = UnrecognizedMessage("")
        return  m ?: raw
    }

    private fun findType(jr: JsonReader): String? {
        jr.beginObject()
        while (jr.hasNext()) {
            if (jr.selectName(labelKeyOption) == -1) {
                jr.skipName()
                jr.skipValue()
                continue
            }

            val labelIndex = jr.selectString(labelOptions)
            if (labelIndex == -1) return null
            jr.close()
            return labels[labelIndex]
        }
        return null
    }
}
