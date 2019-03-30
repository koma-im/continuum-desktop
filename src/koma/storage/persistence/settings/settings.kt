package koma.storage.persistence.settings

import io.requery.Persistable
import io.requery.sql.KotlinEntityDataStore
import javafx.scene.text.Font
import koma.storage.persistence.settings.encoding.ProxyList
import link.continuum.desktop.database.models.dbkv
import kotlin.math.roundToInt




/**
 * settings for the whole app, not specific to a account
 */
class AppSettings(data: KotlinEntityDataStore<Persistable>){

    var proxyList: ProxyList by dbkv(data, ProxyList())
    var scaling: Float by dbkv(data, 1.0f)

    fun scale_em(num: Float) = "${(num * scaling).roundToInt()}em"

    val defaultFontSize = Font.getDefault().size
    val fontSize = defaultFontSize * scaling
}
