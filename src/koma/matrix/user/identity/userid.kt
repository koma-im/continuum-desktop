package koma.matrix.user.identity

import koma.matrix.UserId

fun UserId_new(str: String): UserId? {
    val user_server = str.split(':', limit = 2)
    if (user_server.size != 2)
        return null
    val user = user_server[0].trimStart('@')
    if (!user.all { it.isLetterOrDigit() || "_-\\[]{}^`|=.".contains(it) } || user.isEmpty())
        return null
    val server = user_server[1]
    if (server.isBlank() || !server.all { it.isLetterOrDigit() || it == '.' }) {
        return null
    }
    return UserId(user, server)
}
