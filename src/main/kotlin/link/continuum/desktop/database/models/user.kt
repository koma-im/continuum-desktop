package link.continuum.desktop.database.models

import io.requery.Column
import io.requery.Entity
import io.requery.Key
import io.requery.Persistable
import io.requery.kotlin.eq
import koma.matrix.UserId
import link.continuum.desktop.database.KDataStore
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

@Entity
interface UserToken: Persistable {
    /**
     * user id like @user:matrix.org
     */
    @get:Key
    @get:Column(length = Int.MAX_VALUE)
    var owner: String

    @get:Column(nullable = false, length = Int.MAX_VALUE)
    var token: String
}

fun saveToken(data: KDataStore, userId: UserId, token: String) {
    logger.debug { "saving token of user $userId" }
    val t = UserTokenEntity()
    t.owner = userId.str
    t.token = token
    data.upsert(t)
}


fun getToken(data: KDataStore, userId: UserId): String? {
    return data.select(UserToken::class).where(UserToken::owner.eq(userId.str)).get().firstOrNull()?.token
}
