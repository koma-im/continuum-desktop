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


@Entity
interface SyncBatchKey: Persistable {
    /**
     * used to get the next batch with sync
     */
    @get:Key
    @get:Column(length = Int.MAX_VALUE)
    var owner: String

    @get:Column(nullable = false, length = Int.MAX_VALUE)
    var batch: String
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

fun saveSyncBatchKey(data: KDataStore, userId: UserId, batch: String) {
    logger.debug { "saving sync batch key of user $userId" }
    val t = SyncBatchKeyEntity()
    t.owner = userId.str
    t.batch = batch
    data.upsert(t)
}


fun getSyncBatchKey(data: KDataStore, userId: UserId): String? {
    val k = data.select(SyncBatchKey::class).where(SyncBatchKey::owner.eq(userId.str)).get().firstOrNull()?.batch
    logger.debug { "loaded sync batch key of user $userId: $k" }
    return k
}
