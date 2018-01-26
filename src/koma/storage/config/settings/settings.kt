package koma.storage.config.settings

import koma.storage.config.config_paths
import koma.storage.config.persist.load_json
import koma.storage.config.persist.save_json
import java.io.File

/**
 * settings for the whole app, not specific to a account
 */
object AppSettings{
    private val file by lazy {
        config_paths.getOrCreate("settings")?.let {
            File(it).resolve("koma-settings.json")}
    }

    val settings: AppSettingsData

    init {
        settings = load_json(file, AppSettingsData::class.java) ?: AppSettingsData()
    }

    fun save() {
        save_json(file, settings, AppSettingsData::class.java)
    }
}

class AppSettingsData(
        var fontSize: Float = 2.0f
)

