package koma.gui.view.window.chatroom.messaging.reading.display.room_event.m_message.embed_preview

import com.vdurmont.emoji.EmojiParser
import koma.matrix.UserId
import okhttp3.HttpUrl

private class StringElementTokenizer(private val text: String) {
    val elements = mutableListOf<TextSegment>()

    private var start = 0

    init {
        while (start < text.length) {
            val nls = nextLinkStart()
            if (nls == null) {
                elements.add(PlainTextSegment(text.substring(start)))
                break
            } else {
                if (nls != start) {
                    elements.add(PlainTextSegment( text.substring(start, nls)))
                }
                start = nls
                val nw = nextWhilespace()
                if (nw == null) {
                    elements.add(LinkTextSegment(text.substring(start)))
                    break
                } else {
                    elements.add(LinkTextSegment(text.substring(start, nw)))
                    start = nw
                }
            }
        }
    }

    private fun nextLinkStart(): Int? {
        val nh = text.findAnyOf(listOf("http"), start)?.first
        nh?:return null
        val clearBefore = nh == start || text[nh - 1].isWhitespace()
        val isProto = text.regionMatches(nh + 4, "://", 0, 3)
                || text.regionMatches(nh + 4, "s://", 0, 4)
        if (!clearBefore || !isProto) return null
        return nh
    }

    private fun nextWhilespace(): Int? {
        val i = text.substring(start).indexOfFirst { it.isWhitespace() }
        return if (i < 0) null else i + start
    }
}


private object EmojiSeparator: EmojiParser() {
    fun separateEmojis(textSegment: PlainTextSegment): List<TextSegment> {
        val input = textSegment.text
        var prev = 0;
        val output = mutableListOf<TextSegment>()
        val replacements = getUnicodeCandidates(input)
        for (uc in replacements.iterator()) {
            output.add(PlainTextSegment(input.substring(prev, uc.emojiStartIndex)))
            output.add(EmojiTextSegment(uc.emoji.unicode))
            prev = uc.emojiEndIndex
        }
        output.add(PlainTextSegment(input.substring(prev)))
        return output
    }
}

/**
 * string
 */
sealed class TextSegment()

class PlainTextSegment(
        val text: String
): TextSegment() {
    override fun toString(): String {
        return "plain text: $text"
    }
}

class LinkTextSegment(
        val text: String,
        val url: String? = null
): TextSegment() {
    override fun toString(): String {
        return "link:[$text]($url)"
    }
}

/***
 * any user mentioned by ID
 */
class UserIdLink(
        val userId: UserId,
        val text: String,
        val url: String? = null
): TextSegment() {
    override fun toString() = "UserId:$userId($text,$url)"
}

class EmojiTextSegment(
        val emoji: String
): TextSegment() {
    override fun toString(): String {
        return "emoji: $emoji"
    }
}

fun tokenize_string(input: String): List<TextSegment> {
    val textAndLinks = StringElementTokenizer(input).elements
    val segments= textAndLinks.flatMap { textSegment: TextSegment ->
        if (textSegment is PlainTextSegment) {
            EmojiSeparator.separateEmojis(textSegment)
        } else {
            listOf(textSegment)
        }
    }
    return segments
}

