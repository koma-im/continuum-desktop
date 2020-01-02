package link.continuum.desktop.database

import io.requery.Persistable
import io.requery.kotlin.Logical
import io.requery.kotlin.desc
import io.requery.kotlin.eq
import io.requery.kotlin.lte
import io.requery.sql.KotlinEntityDataStore
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.actor
import link.continuum.database.models.RoomEventRow

class KDataStore(
        @Deprecated("internal")
        val _dataStore: KotlinEntityDataStore<Persistable>
) {
    private val scope = CoroutineScope(Dispatchers.IO)
    val actor = scope.actor<DbMsg>(capacity = 8) {
        for (msg in channel) {
            @Suppress("DEPRECATION")
            when (msg) {
                is DbMsg.Operation -> {
                    msg.operation(_dataStore)
                }
                is DbMsg.UpdateEvents -> {
                    _dataStore.upsert(msg.events)
                }
                is DbMsg.LoadEvents -> {
                    var condition: Logical<*, *> = RoomEventRow::room_id.eq(msg.roomId)
                    if (msg.upto!=null) {
                        condition = condition.and(RoomEventRow::server_time.lte(msg.upto))
                    }
                    val r = _dataStore.select(RoomEventRow::class).where(condition)
                            .orderBy(RoomEventRow::server_time.desc()).limit(msg.limit).get().reversed()
                    msg.response.complete(r)
                }
            }
        }
    }
    suspend inline fun <T> runOp(crossinline op: suspend KotlinEntityDataStore<Persistable>.() -> T): T {
        val deferred = CompletableDeferred<T>()
        val msg = DbMsg.Operation {
            deferred.complete(it.op())
        }
        actor.send(msg)
        val v = deferred.await()
        return v
    }
    suspend inline fun <T> letOp(crossinline op: suspend (KotlinEntityDataStore<Persistable>) -> T): T {
        return runOp {
            op(this)
        }
    }
    suspend fun updateEvents(events: List<RoomEventRow>) {
        actor.send(DbMsg.UpdateEvents(events))
    }
    /**
     * get events not newer than upto
     */
    suspend fun loadEvents(roomId: String,
                           upto: Long?,
                           limit: Int
    ):List<RoomEventRow>  {
        val deferred = CompletableDeferred<List<RoomEventRow>>()
        actor.send(DbMsg.LoadEvents(roomId, upto, limit, deferred))
        val v = deferred.await()
        return v
    }
    fun close() {
        @Suppress("DEPRECATION")
        _dataStore.close()
    }

    sealed class DbMsg {
        class Operation(
                val operation: suspend (KotlinEntityDataStore<Persistable>) -> Unit
        ) : DbMsg()
        class UpdateEvents(val events: List<RoomEventRow>): DbMsg()
        class LoadEvents(val roomId: String,
                         val upto: Long?,
                         val limit: Int,
                         val response: CompletableDeferred<List<RoomEventRow>>
        ): DbMsg()
    }

}

