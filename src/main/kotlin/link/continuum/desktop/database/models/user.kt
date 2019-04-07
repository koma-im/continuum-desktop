package link.continuum.desktop.database.models

import io.requery.*
import io.requery.kotlin.desc
import io.requery.kotlin.eq
import koma.matrix.UserId
import link.continuum.desktop.database.KDataStore
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

@Entity
interface UserNickname: Persistable {
    /**
     * user id like @user:matrix.org
     */
    @get:Index()
    @get:Column(length = Int.MAX_VALUE, nullable = false)
    var owner: String

    @get:Column(length = Int.MAX_VALUE, nullable = false)
    var nickname: String

    @get:Column(length = Int.MAX_VALUE, nullable = false)
    var since: Long
}

@Entity
interface UserAvatar: Persistable {
    /**
     * user id like @user:matrix.org
     */
    @get:Index()
    @get:Column(length = Int.MAX_VALUE, nullable = false)
     var key: String

    /**
     * URL
     */
    @get:Column(length = Int.MAX_VALUE, nullable = false)
     var avatar: String

    @get:Column(length = Int.MAX_VALUE, nullable = false)
    var since: Long
}

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

/**
 * recently used accounts
 */
@Entity
interface AccountUsage: Persistable {
    /**
     * user id like @user:matrix.org
     */
    @get:Key
    @get:Column(length = Int.MAX_VALUE)
    var owner: String

    @get:Column(nullable = false)
    var usage: Long
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

fun saveUserNick(data: KDataStore, userId: UserId, nick: String, timestamp: Long) {
    val d = data.select(UserNickname::class) where (UserNickname::owner.eq(userId.str)
            and UserNickname::nickname.eq(nick)
            and UserNickname::since.eq(timestamp)
            )
    if (d.get().firstOrNull() != null) {
        logger.trace { "already saved nickname $nick of user $userId with time $timestamp" }
        return
    }
    val t: UserNickname = UserNicknameEntity()
    t.owner = userId.str
    t.nickname = nick
    t.since = timestamp
    data.insert(t)
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

fun getLatestNick(data: KDataStore, userId: UserId): UserNickname? {
    return data.select(UserNickname::class)
            .where(UserNickname::owner.eq(userId.str))
            .orderBy(UserNickname::since.desc())
            .get().firstOrNull()
}

fun getRecentUsers(data: KDataStore): List<UserId> {
    return data.select(AccountUsage::class)
            .orderBy(AccountUsage::usage.desc()).limit(10).get().map { UserId(it.owner) }
}

fun updateAccountUsage(data: KDataStore, userId: UserId) {
    val r: AccountUsage = AccountUsageEntity()
    r.owner = userId.str
    r.usage = System.currentTimeMillis()
    data.upsert(r)
}

fun saveSyncBatchKey(data: KDataStore, userId: UserId, batch: String) {
    logger.trace { "saving sync batch key of user $userId" }
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

fun saveUserAvatar(data: KDataStore, userId: UserId, avatar: String, timestamp: Long) {
    val d = data.select(UserAvatar::class) where (UserAvatar::key.eq(userId.str)
            and UserAvatar::avatar.eq(avatar)
            and UserAvatar::since.eq(timestamp)
            )
    if (d.get().firstOrNull() != null) {
        logger.trace { "already saved Avatar $avatar of user $userId with time $timestamp" }
        return
    }
    val t: UserAvatar = UserAvatarEntity()
    t.key = userId.str
    t.avatar = avatar
    t.since = timestamp
    data.insert(t)
}

fun getLatestAvatar(data: KDataStore, userId: UserId): UserAvatar? {
    return data.select(UserAvatar::class)
            .where(UserAvatar::key.eq(userId.str))
            .orderBy(UserAvatar::since.desc())
            .get().firstOrNull()
}
