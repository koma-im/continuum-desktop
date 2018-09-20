package koma.util.coroutine.observable

import javafx.beans.value.ObservableValue
import javafx.beans.value.WeakChangeListener
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ClosedSendChannelException
import kotlinx.coroutines.channels.produce
import tornadofx.*

fun<T> ObservableValue<T>.updates() = GlobalScope.produce<T>(capacity = Channel.CONFLATED){
    val waitClose = CompletableDeferred<Unit>()
    val listener = ChangeListener { _, _, newValue: T ->
        try {
            offer(newValue)
        } catch (e: ClosedSendChannelException) {
            waitClose.complete(Unit)
        }
    }
    this@updates.addListener(WeakChangeListener<T>(listener))
    waitClose.await()
}
