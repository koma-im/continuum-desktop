package koma.gui.element.emoji.category

data class EmojiSymbol(
        val glyph: String,
        val codepoint: String,
        val description: String
)

data class EmojiCategory(
        val name: String,
        val emojis: List<EmojiSymbol>
)

val emojiCategories = listOf(
        EmojiCategory("People", People),
        EmojiCategory("Nature", Nature),
        EmojiCategory("Food", Food),
        EmojiCategory("Activities", Activities),
        EmojiCategory("Travel", Travel),
        EmojiCategory("Objects", Objects),
        EmojiCategory("Symbols", Symbols),
        EmojiCategory("Flags", Flags)
)
