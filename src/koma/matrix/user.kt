package koma.matrix

import koma.matrix.json.NewTypeString
import koma.storage.users.UserStore

data class UserId(private val input: String): NewTypeString(input), Comparable<UserId> {

    val user: String
    val server: String

    init {
        val s = str.trimStart('@')
        user = s.substringBefore(':')
        server = s.substringAfter(':')
    }

    override fun compareTo(other: UserId): Int = this.str.compareTo(other.str)

    fun getState() = UserStore.getOrCreateUserId(this)
}

