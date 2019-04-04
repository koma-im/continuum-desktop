package koma.storage.persistence.account

import koma.matrix.UserId
import koma.storage.config.ConfigPaths
import mu.KotlinLogging


private val logger = KotlinLogging.logger {}

fun ConfigPaths.userProfileDir(userid: UserId): String? {
    return this.getCreateProfileDir(userid.server, userid.user)
}
