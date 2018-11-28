package koma.matrix.event.room_message

import com.squareup.moshi.JsonReader

class JsonKeyFinder(
        labelKey: String
) {
    private val labelKeyOption = JsonReader.Options.of(labelKey)

    fun find(jr: JsonReader): String? {
        jr.beginObject()
        while (jr.hasNext()) {
            if (jr.selectName(labelKeyOption) == -1) {
                jr.skipName()
                jr.skipValue()
                continue
            }

            return jr.nextString()
        }
        return null
    }
}
