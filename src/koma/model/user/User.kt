package koma.model.user

import javafx.beans.property.SimpleLongProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.scene.paint.Color
import koma.gui.element.icon.placeholder.generator.hashStringColorDark
import koma.koma_app.SaveJobs
import koma.matrix.UserId
import koma.matrix.user.presence.UserPresenceType
import koma.storage.users.state.saveUser
import koma_app.appState

/**
 * Created by developer on 2017/6/25.
 */
data class UserState(val id: UserId) {
    var modified = true

    val present = SimpleObjectProperty<UserPresenceType>(UserPresenceType.Offline)

    var name: String
        set(value) {
            this.modified = true
            this.displayName.set(value)
        }
        get() = this.displayName.get()
    val displayName = SimpleStringProperty(id.toString())

    val color = hashStringColorDark(id.toString())
    val colorProperty = SimpleObjectProperty<Color>(color)

    var avatar: String
        set(value) {
            this.modified = true
            this.avatarURL.set(value)
        }
        get() = this.avatarURL.get()
    val avatarURL = SimpleStringProperty("");

    val lastActiveAgo = SimpleLongProperty(Long.MAX_VALUE)

    init {
        SaveJobs.addJob { if (this.modified) appState.koma.paths.saveUser(this) }
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
