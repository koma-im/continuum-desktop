package koma.gui.view.window.chatroom.messaging.reading.display.room_event.m_message.embed_preview

class StringElementTokenizer(private val text: String) {
    val elements = mutableListOf<TextSegment>()

    private var start = 0

    init {
        while (start < text.length) {
            val nls = nextLinkStart()
            if (nls == null) {
                elements.add(TextSegment(TextSegmentKind.Plain, text.substring(start)))
                break
            } else {
                if (nls != start) {
                    elements.add(TextSegment(TextSegmentKind.Plain, text.substring(start, nls)))
                }
                start = nls
                val nw = nextWhilespace()
                if (nw == null) {
                    elements.add(TextSegment(TextSegmentKind.Link, text.substring(start)))
                    break
                } else {
                    elements.add(TextSegment(TextSegmentKind.Link, text.substring(start, nw)))
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

class TextSegment(
        val kind: TextSegmentKind,
        val text: String
) {
    override fun toString(): String {
        return "$kind text: $text"
    }
}

enum class TextSegmentKind {
        Plain,
        Link,
}
