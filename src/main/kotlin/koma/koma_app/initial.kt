package koma.koma_app

import io.requery.Persistable
import io.requery.sql.KotlinEntityDataStore
import koma.storage.persistence.settings.AppSettings
import link.continuum.database.openStore
import java.io.File
import kotlin.time.ExperimentalTime

/**
 * load proxy settings early, needed to initialize http client
 */
@ExperimentalTime
fun loadSettings(dir: File): Pair<AppSettings, KotlinEntityDataStore<Persistable>>{
    val desktop = dir.resolve("desktop")
    desktop.mkdirs()
    val dbPath = desktop.resolve("continuum-desktop").canonicalPath
    val db = openStore(dbPath)
    val settings = AppSettings(db)
    return settings to db
}