package koma.matrix.user.identity

import koma.matrix.UserId

fun UserId_new(str: String): UserId {
    return UserId(str)
}

fun isUserIdValid(input: String): Boolean {
    if (input.isNotBlank())
        return true
    return false
}
