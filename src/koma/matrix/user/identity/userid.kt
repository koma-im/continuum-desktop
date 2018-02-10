package koma.matrix.user.identity

import koma.matrix.UserId

fun UserId_new(str: String): UserId {
    val (user, server) = str.trimStart('@').let {
        Pair(it.substringBefore(':'), it.substringAfter(':'))
    }
    return UserId(user, server)
}

fun isUserIdValid(input: String): Boolean {
    if (input.isNotBlank())
        return true
    return false
}
