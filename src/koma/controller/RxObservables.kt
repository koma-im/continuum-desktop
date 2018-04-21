package controller

import javafx.event.ActionEvent
import rx.javafx.sources.CompositeObservable

/**
 * Created by developer on 2017/7/6.
 */
object guiEvents {
    val updateAvatar = CompositeObservable<ActionEvent>()
}
