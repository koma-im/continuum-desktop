package koma.storage.persistence.util

import koma.matrix.json.MoshiInstance
import koma.storage.persistence.settings.encoding.ProxyAdapter

object AppMoshi {
    val moshi = MoshiInstance.moshiBuilder.add(ProxyAdapter()).build()
}
