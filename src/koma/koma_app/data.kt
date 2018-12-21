package koma.koma_app

import koma.storage.config.ConfigPaths
import koma.storage.persistence.settings.AppSettings

/**
 * global variable of data
 */
lateinit var appData: DataOnDisk

/**
 * data saved on disk
 */
class DataOnDisk(paths: ConfigPaths) {
    val settings: AppSettings by lazy { AppSettings(paths) }
}
