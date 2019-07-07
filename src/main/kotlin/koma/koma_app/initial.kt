package koma.koma_app

import io.requery.Persistable
import io.requery.sql.KotlinEntityDataStore
import koma.storage.persistence.settings.AppSettings
import link.continuum.database.openStore
import java.io.File

/**
 * load proxy settings early, needed to initialize http client
 */
fun loadSettings(dir: String): Pair<AppSettings, KotlinEntityDataStore<Persistable>>{
    val db = loadDb(dir)
    val settings = AppSettings(db)
    return settings to db
}

private fun loadDb(dir: String): KotlinEntityDataStore<Persistable> {
    val desktop = File(dir).resolve("desktop")
    desktop.mkdirs()
    val dbPath = desktop.resolve("continuum-desktop").canonicalPath
    return openStore(dbPath)
}