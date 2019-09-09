package link.continuum.desktop.gui

import javafx.application.Application
import javafx.geometry.Insets
import javafx.scene.Node
import javafx.scene.control.Alert
import javafx.scene.control.ButtonType
import javafx.scene.layout.Background
import javafx.scene.layout.BackgroundFill
import javafx.scene.layout.CornerRadii
import javafx.scene.paint.Color
import javafx.stage.Stage
import javafx.stage.Window
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.javafx.JavaFx
import kotlinx.coroutines.selects.select
import link.continuum.desktop.util.None
import link.continuum.desktop.util.Option
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

val whiteBackGround = Background(BackgroundFill(Color.WHITE, CornerRadii.EMPTY, Insets.EMPTY))

object JFX {
    lateinit var primaryStage: Stage
    lateinit var application: Application
    val hostServices by lazy { application.hostServices }
}

fun<T: Node> T.showIf(show: Boolean) {
    this.isManaged = show
    this.isVisible = show
}

val UiDispatcher = Dispatchers.JavaFx

fun uialert(type: Alert.AlertType,
            header: String,
            content: String? = null,
            vararg buttons: ButtonType,
            owner: Window? = null,
            title: String? = null,
            actionFn: Alert.(ButtonType) -> Unit = {}) {
    GlobalScope.launch(UiDispatcher) {
        val alert = Alert(type, content ?: "", *buttons)
        title?.let { alert.title = it }
        alert.headerText = header
        owner?.also { alert.initOwner(it) }
        val buttonClicked = alert.showAndWait()
        if (buttonClicked.isPresent) {
            alert.actionFn(buttonClicked.get())
        }
    }
}

/**
 * convert a channel of unordered updates with timestamps
 * to one that provides the latest value
 */
@ExperimentalCoroutinesApi
class UpdateConflater<T> {
    private val inputs = Channel<Pair<Long, T>>()
    private val updates = ConflatedBroadcastChannel<T>()

    suspend fun update(time: Long, value: T) {
        inputs.send(time to value)
    }
    fun subscribe(): ReceiveChannel<T> {
        return updates.openSubscription()
    }

    init {
        GlobalScope.launch { processUpdates() }
    }
    private fun CoroutineScope.processUpdates() {
        var latest = 0L
        var lastValue: T? = null
        launch {
            for ((t, v) in inputs) {
                if (t >= latest) {
                    if (v != lastValue) {
                        logger.trace { "value $v updated at $t, newer than $latest" }
                        latest = t
                        lastValue = v
                        updates.send(v)
                    }
                } else {
                    logger.info { "value $v comes at $t, older than $latest" }
                }
            }
        }
    }
}

fun<T, U, C: SendChannel<U>> CoroutineScope.switchGetDeferred(
        input: ReceiveChannel<T>,
        getDeferred: (T)->Deferred<U>,
        output: C
) {
    launch {
        var current = input.receive()
        while (isActive) {
            val next = select<T?> {
                input.onReceiveOrNull { update ->
                    update
                }
                getDeferred(current).onAwait {
                    output.send(it)
                    input.receiveOrNull()
                }
            }
            if (next == null) {
                logger.debug { "no more input after $current" }
                break
            } else {
                current = next
            }
        }
        logger.debug { "closing switchMapDeferred" }
        input.cancel()
        output.close()
    }
}


fun<T: Any, U: Any, C: SendChannel<Option<U>>> CoroutineScope.switchGetDeferredOption(
        input: ReceiveChannel<Option<T>>,
        getDeferred: (T)->Deferred<Option<U>>,
        output: C
) {
    launch {
        var current = input.receive()
        while (isActive) {
            val next = select<Option<T>?> {
                input.onReceiveOrNull { it }
                current.map { getDeferred(it) }.map {
                    it.onAwait {
                        output.send(it)
                        input.receiveOrNull()
                    }
                }
            }
            if (next == null) {
                logger.debug { "no more input after $current" }
                break
            } else {
                current = next
                if (current.isEmpty)
                    output.send(None())
            }
        }
        logger.debug { "closing switchMapDeferred" }
        input.cancel()
        output.close()
    }
}

/**
 * switch channel by key
 */
@ExperimentalCoroutinesApi
fun <T, U> CoroutineScope.switchUpdates(
        input: ReceiveChannel<T>,
        getUpdates: (T)->ReceiveChannel<U>
): ReceiveChannel<U> {
    val output = Channel<U>(Channel.CONFLATED)
    launch {
        var key = input.receive()
        var updates = getUpdates(key)
        var latest: U? = null
        loop@ while (isActive) {
            val state = select<UpdateState<T>> {
                input.onReceiveOrNull { k ->
                    k?.let {
                        logger.trace { "switching to updates for $k" }
                        UpdateState.Switch<T>(it)
                    } ?: UpdateState.Close()
                }
                updates.onReceive {
                    if (it!= latest) {
                        latest = it
                        logger.trace { "got update for $key: $it" }
                        output.send(it)
                    }
                    UpdateState.Continue()
                }
            }
            when (state) {
                is UpdateState.Switch<T> -> {
                    updates.cancel()
                    key = state.key
                    updates = getUpdates(key)
                }
                is UpdateState.Close -> {
                    logger.debug { "no more updates after $key" }
                    break@loop
                }
                is UpdateState.Continue -> {
                }
            }
        }
        logger.debug { "canceling subscription" }
        input.cancel()
        output.close()
    }
    return output
}

sealed class UpdateState<T> {
    class Switch<T>(val key: T): UpdateState<T>()
    class Close<T>: UpdateState<T>()
    class Continue<T>: UpdateState<T>()
}
