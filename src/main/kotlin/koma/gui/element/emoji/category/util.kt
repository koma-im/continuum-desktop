package koma.gui.element.emoji.category

data class EmojiCategory(
        val name: String,
        val emojis: List<String>
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
