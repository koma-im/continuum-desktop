package koma.model.user

import javafx.beans.property.SimpleLongProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.scene.image.Image
import javafx.scene.paint.Color
import koma.graphic.getImageForName
import koma.graphic.getResizedImage
import koma.graphic.hashStringColorDark
import koma.matrix.UserId
import koma.matrix.user.presence.UserPresenceType
import rx.javafx.kt.observeOnFx
import rx.javafx.kt.toObservable
import rx.lang.kotlin.filterNotNull
import rx.schedulers.Schedulers

/**
 * Created by developer on 2017/6/25.
 */
data class UserState(val id: UserId) {
    val present = SimpleObjectProperty<UserPresenceType>(UserPresenceType.Offline)
    val displayName = SimpleStringProperty(id.toString())
    val color = hashStringColorDark(id.toString())
    val colorProperty = SimpleObjectProperty<Color>(color)
    val avatarURL = SimpleStringProperty("");
    val lastActiveAgo = SimpleLongProperty(Long.MAX_VALUE)

    var has_avatar = false
    val avatarImgProperty = SimpleObjectProperty<Image>(getImageForName(id.user, color))
    var avatar_img: Image
        set(value) {this.avatarImgProperty.set(value)}
        get() = this.avatarImgProperty.get()

    init {
        avatarURL.toObservable().filter { it.isNotBlank() }.observeOn(Schedulers.io())
                .map {
                    println("User $this has new avatar url $it")
                    getResizedImage(it, 32.0, 32.0)
                }
                .filterNotNull()
                .observeOnFx()
                .subscribe {
                    avatar_img = it
                    has_avatar = true
                }
        val nameobserv = displayName.toObservable()
        nameobserv.filter{ it.isNotBlank() && !has_avatar}
                .map { getImageForName(it, color) }
                .subscribe { avatarImgProperty.set(it) }
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
