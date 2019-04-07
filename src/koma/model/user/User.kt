package koma.model.user

import javafx.beans.property.ReadOnlyObjectWrapper
import javafx.beans.property.SimpleLongProperty
import javafx.beans.property.SimpleObjectProperty
import koma.gui.element.icon.placeholder.generator.hashStringColorDark
import koma.matrix.UserId
import koma.matrix.user.presence.UserPresenceType
import link.continuum.desktop.database.KDataStore
import link.continuum.desktop.database.models.saveUserAvatar
import okhttp3.HttpUrl

/**
 * Created by developer on 2017/6/25.
 */
data class UserState(val id: UserId,
                     private val data: KDataStore
) {
    val present = SimpleObjectProperty<UserPresenceType>(UserPresenceType.Offline)

    val color = hashStringColorDark(id.toString())

    private val _avatar = ReadOnlyObjectWrapper<HttpUrl>()
    val avatar = _avatar.readOnlyProperty

    val lastActiveAgo = SimpleLongProperty(Long.MAX_VALUE)

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
