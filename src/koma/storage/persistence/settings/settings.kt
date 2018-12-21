package koma.storage.persistence.settings

import javafx.scene.text.Font
import koma.storage.config.ConfigPaths
import koma.storage.persistence.util.load_json
import koma.storage.persistence.util.save_json
import java.io.File
import java.net.Proxy
import kotlin.math.roundToInt


/**
 * settings for the whole app, not specific to a account
 */
class AppSettings(paths: ConfigPaths){
    private val file by lazy {
        paths.getOrCreate("settings")?.let {
            File(it).resolve("koma-settings.json")}
    }

    val settings: AppSettingsData

    init {
        settings = load_json(file, AppSettingsData::class.java) ?: AppSettingsData()
        if (!settings.proxies.contains(Proxy.NO_PROXY))
            settings.proxies.add(Proxy.NO_PROXY)
    }

    fun save() {
        // several most recent
        settings.proxies = settings.proxies.take(5).toMutableList()
        save_json(file, settings, AppSettingsData::class.java)
    }

    val scaling
        get() = settings.scaling

    fun scale_em(num: Float) = "${(num * settings.scaling).roundToInt()}em"

    val defaultFontSize = Font.getDefault().size
    val fontSize = defaultFontSize * scaling

    /**
     * get preferred proxy
     */
    fun getProxy(): Proxy {
        return settings.proxies.getOrNull(0)?: Proxy.NO_PROXY
    }

    fun set_preferred_proxy(proxy: Proxy) {
        this.settings.proxies.remove(proxy)
        this.settings.proxies.add(0, proxy)
    }
}

class AppSettingsData(
        var proxies: MutableList<Proxy> = mutableListOf(Proxy.NO_PROXY),
        var scaling: Float = 1.0f
)
