package koma.storage.persistence.settings

import javafx.scene.text.Font
import kotlin.math.roundToInt




/**
 * settings for the whole app, not specific to a account
 */
class AppSettings{
    var scaling: Float = 1.0f

    fun scale_em(num: Float) = "${(num * scaling).roundToInt()}em"

    val defaultFontSize = Font.getDefault().size
    val fontSize = defaultFontSize * scaling
}
