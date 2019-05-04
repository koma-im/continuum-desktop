package koma.gui.element.icon.user

/**
 * extract characters for drawing a placeholder avatar
  */
fun extract_key_chars(fullname: String): Pair<Char, Char> {
    // this is a workaround, really the irc bridge needs improvements
    val name = fullname.replace(" (IRC)", "")
    val first = name.getOrNull(0) ?: ' '
    val second = name.substringAfter(' ', "")
            .getOrNull(0) ?: name.getOrNull(1) ?: ' '
    return Pair(first, second)
}
