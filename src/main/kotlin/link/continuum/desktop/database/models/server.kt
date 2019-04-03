package link.continuum.desktop.database.models

import io.requery.Column
import io.requery.Entity
import io.requery.Key
import io.requery.Persistable
import io.requery.kotlin.desc
import io.requery.kotlin.eq
import link.continuum.desktop.database.KDataStore


@Entity
interface ServerAddress: Persistable {
    @get:Key
    @get:Column(length = Int.MAX_VALUE)
    var name: String

    @get:Key
    @get:Column(length = Int.MAX_VALUE)
    var address: String

    var lastUse: Long?
}

fun saveServerAddr(data: KDataStore, name: String, address: String) {
    val serverAddress = ServerAddressEntity()
    serverAddress.name = name
    serverAddress.address = address
    serverAddress.lastUse = System.currentTimeMillis()
    data.upsert(serverAddress)
}

fun getServerAddrs(data: KDataStore, name: String): List<String> {
    return data.select(ServerAddress::class).where(
            ServerAddress::name.eq(name)
    ).orderBy(ServerAddress::lastUse.desc()).limit(5).get().map { it.address }
}
