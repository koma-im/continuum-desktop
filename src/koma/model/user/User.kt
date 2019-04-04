package koma.model.user

import javafx.beans.property.*
import javafx.scene.paint.Color
import koma.gui.element.icon.placeholder.generator.hashStringColorDark
import koma.matrix.UserId
import koma.matrix.user.presence.UserPresenceType
import link.continuum.desktop.database.KDataStore
import link.continuum.desktop.database.models.saveUserAvatar
import link.continuum.desktop.database.models.saveUserNick
import okhttp3.HttpUrl

/**
 * Created by developer on 2017/6/25.
 */
data class UserState(val id: UserId,
                     private val data: KDataStore
) {
    val present = SimpleObjectProperty<UserPresenceType>(UserPresenceType.Offline)

    private val _name = ReadOnlyStringWrapper(id.str)
    val name: ReadOnlyStringProperty =  _name.readOnlyProperty

    val color = hashStringColorDark(id.toString())
    val colorProperty = SimpleObjectProperty<Color>(color)

    private val _avatar = ReadOnlyObjectWrapper<HttpUrl>()
    val avatar = _avatar.readOnlyProperty

    val lastActiveAgo = SimpleLongProperty(Long.MAX_VALUE)

    fun setName(name: String, timestamp: Long) {
        _name.set(name)
        saveUserNick(data, id, name, timestamp)
    }

    fun setAvatar(url: HttpUrl, timestamp: Long) {
        _avatar.set(url)
        saveUserAvatar(data, id, url.toString(), timestamp)
    }

    fun weight(): Int {
        val la = lastActiveAgo.get()
        val SECONDS_PER_YEAR = (60L * 60L * 24L * 365L)
        val SECONDS_PER_DECADE = (10L * SECONDS_PER_YEAR)
        val laSec = Math.min(la.toLong() / 1000, SECONDS_PER_DECADE)
        var result = (1 + (SECONDS_PER_DECADE - laSec )).toInt()
        if (present.get() == UserPresenceType.Online) {
            result *= 2
        }
        return result
    }

    override fun toString() = "$id ${present.get()} ${weight()}"

}
