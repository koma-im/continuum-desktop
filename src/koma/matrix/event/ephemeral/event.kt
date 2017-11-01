package koma.matrix.epemeral

import com.squareup.moshi.Json

/**
 * because of restrictions of moshi, sometimes we have to deal with Map
 * manually and convert it to a nice type
 */
data class EphemeralRawEvent(
        val type: EphemeralRawEventType,
        val content: Map<String, Any>
)

enum class EphemeralRawEventType {
    @Json(name = "m.tying") Typing
}

sealed class EphemeralEvent()

data class TypingEvent(val user_ids: List<String>): EphemeralEvent()

fun EphemeralRawEvent.parse(): EphemeralEvent? {
    when(this.type) {
        EphemeralRawEventType.Typing -> {
            val userIds = this.content["user_ids"]
            if (userIds != null && userIds is List<*>) {
                val usersTyping: List<String> = userIds.map { it.toString() }
                return TypingEvent(usersTyping)
            } else {
                return null
            }
        }
        else -> {
            return null
        }
    }
}
