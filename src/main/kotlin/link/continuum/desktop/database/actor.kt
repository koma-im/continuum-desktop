package link.continuum.desktop.database

import io.requery.Persistable
import io.requery.sql.KotlinEntityDataStore
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.actor

class KDataStore(
        @Deprecated("internal")
        val _dataStore: KotlinEntityDataStore<Persistable>
) {
    private val scope = CoroutineScope(Dispatchers.IO)
    val actor = scope.actor<DbMsg>(capacity = 8) {
        for (msg in channel) {
            when (msg) {
                is DbMsg.Operation -> {
                    @Suppress("DEPRECATION")
                    msg.operation(_dataStore)
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
    fun close() {
        @Suppress("DEPRECATION")
        _dataStore.close()
    }

    sealed class DbMsg {
        class Operation(
                val operation: suspend (KotlinEntityDataStore<Persistable>) -> Unit
        ) : DbMsg()
    }

}

