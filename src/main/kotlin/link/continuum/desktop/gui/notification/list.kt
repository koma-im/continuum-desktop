@file:Suppress("EXPERIMENTAL_API_USAGE")

package link.continuum.desktop.gui.notification

import javafx.application.Platform
import javafx.collections.FXCollections
import javafx.collections.ListChangeListener
import javafx.collections.WeakListChangeListener
import javafx.collections.transformation.SortedList
import javafx.event.ActionEvent
import javafx.event.EventHandler
import javafx.geometry.Pos
import javafx.scene.Group
import javafx.scene.control.*
import javafx.scene.layout.Priority
import javafx.scene.text.Text
import javafx.scene.text.TextFlow
import javafx.util.Callback
import koma.Failure
import koma.Server
import koma.gui.view.window.chatroom.messaging.reading.display.room_event.m_message.content.MessageView
import koma.gui.view.window.chatroom.messaging.reading.display.room_event.util.DatatimeView
import koma.koma_app.AppStore
import koma.matrix.NotificationResponse
import koma.matrix.UserId
import koma.matrix.event.room_message.RoomEventType
import koma.util.testFailure
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import link.continuum.desktop.gui.*
import link.continuum.desktop.gui.icon.avatar.AvatarView
import link.continuum.desktop.util.Account
import link.continuum.desktop.util.gui.alert
import mu.KotlinLogging

private typealias Notification = NotificationResponse.Notification
private typealias Item = Notification
private typealias List = ListView<Item>

private val logger = KotlinLogging.logger {}

/**
 * manages notifications of an account
 */
private class AccountNotifications(
        internal val account: Account,
        private val listView: List
): CoroutineScope by CoroutineScope(Dispatchers.Default) {
    private val notificationsUnordered = FXCollections.observableArrayList<Notification>()
    val list = SortedList(notificationsUnordered, Comparator { t, t2 ->
        t.ts.compareTo(t2.ts)
    })
    val placeholderView = PlaceholderView(this)
    val fetchStatusView = PaginationStatusView(this)
    private var state: NState = NState.Empty
        get() = field
        private set(value) {
            field = value
            placeholderView.updateState(value)
            fetchStatusView.updateState(value)
            logger.debug { "status $value" }
        }
    private var next_token: String? = null

    private val onListChange = ListChangeListener<Notification> {
        fetchStatusView.root.showIf(list.size > 0)
    }
    init {
        fetchStatusView.root.showIf(false)
        list.addListener(WeakListChangeListener(onListChange))
    }

    /**
     * when the list of notifications comes into view
     */
    fun view() {
        when (state) {
            is NState.Empty, is NState.Failed -> {
                logger.debug { "viewing incomplete notifications"}
                fetch()
            }
        }
    }
    private var tailItems = listOf<Long>()
    /**
     * when a notification becomes visible
     * fetch more if it is near the end
     */
    fun updateVisible(notification: Notification) {
        if (!tailItems.any { it == notification.ts }) return
        logger.trace { "nearly reached end of notifications, fetching more"}
        fetch()
    }
    /**
     * get notifications
     */
    fun fetch() {
        check(Platform.isFxApplicationThread())
        if (state is NState.Loading) return
        if (state is NState.Finished) {
            return
        }
        state = NState.Loading
        launch(Dispatchers.Main) {
            logger.debug { "getting notifications from $next_token"}
            val (success, failure, r) = account.getNotifications(limit=20, from=next_token)
            if (r.testFailure(success, failure)) {
                state = NState.Failed(failure)
                logger.error { "error $r"}
            } else {
                val t = success.next_token
                next_token = t
                tailItems = success.notifications.map { it.ts }.sorted().take(5)
                val loadedNum = notificationsUnordered.size
                notificationsUnordered.addAll(success.notifications)
                if (t != null) {
                    state = NState.Idle
                } else {
                    logger.debug { "reached end of all notifications"}
                    state = NState.Finished
                }
                val currentNum = notificationsUnordered.size
                if (loadedNum == 0 && currentNum > 0) {
                    if (listView.items === list) {
                        logger.trace { "scrollTo $currentNum"}
                        listView.scrollTo(currentNum - 1)
                    } else {
                        logger.trace { "not active"}
                    }
                } else {
                    logger.trace { "notifications size from $loadedNum to $currentNum"}
                }
            }
        }
    }
}

/**
 * status when paging through notifications
 */
private class PaginationStatusView(private val data: AccountNotifications) {
    val loadingView = TextFlow(Text("Getting more notifications")).apply {
        minWidth = 15.0
    }
    var failedView = PlaceholderView.FailedView(data)
    val root = StackPane().apply {
        background = whiteBackGround
    }
    fun updateState(state: NState) {
        root.children.clear()
         when (state) {
            is NState.Loading -> {
                root.children.setAll(loadingView)
            }
            is NState.Failed -> {
                failedView.failure = state.failure
                root.children.setAll(failedView)
            }
        }
    }
}

/**
 * status when no notifications have been fetched
 */
private class PlaceholderView(private val data: AccountNotifications) {
    val emptyView = VBox(5.0,
            TextFlow(Text("Notifications have not been loaded yet.")).apply {
                minWidth = 15.0
            },
            Hyperlink("Refresh").apply {
                setOnAction {
                    data.fetch()
                }
            }
    ).apply { alignment = Pos.CENTER}
    val root = Group(emptyView)
    val loadingView = TextFlow(Text("Getting notifications"))
    var failedView = FailedView(data)
    val finishedView = VBox(5.0,
            TextFlow(Text("There are no more notifications for now.")),
            Hyperlink("Refresh").apply {
                setOnAction { data.fetch() }
            }
    )

    fun updateState(state: NState) {
        when (state) {
            is NState.Idle -> {
                root.children.setAll(emptyView)
            }
            is NState.Loading -> root.children.setAll(loadingView)
            is NState.Failed -> {
                failedView.failure = state.failure
                root.children.setAll(failedView)
            }
            is NState.Finished -> root.children.setAll(finishedView)
        }
    }
    class FailedView(var data: AccountNotifications) : VBox() {
        var failure: Failure? = null
        val showFailure = EventHandler<ActionEvent> {
            alert(Alert.AlertType.ERROR,
                    "Couldn't get notifications", "$failure")
        }
        init {
            alignment = Pos.CENTER
            children.addAll(
                    TextFlow(Text("Failed to get more notifications.")),
                    Hyperlink("Retry").apply {
                        setOnAction { data.fetch() }
                    },
                    Hyperlink("Info").apply {
                        onAction = showFailure
                    }
            )
        }
    }
}

private sealed class NState {
    object Empty : NState()
    object Idle : NState()
    object Loading : NState()
    class Failed(val failure: Failure) : NState()
    object Finished : NState()
}

class NotificationList(
        store: AppStore
): CoroutineScope by CoroutineScope(Dispatchers.Default) {
    private val accounts = hashMapOf<UserId, AccountNotifications>()
    private val context = ListContext()
    private var currentAccount: UserId? = null

    private val placeholderBox = VBox().apply { alignment = Pos.CENTER }
    private val listView = List().apply{
        placeholder = HBox().apply {
            alignment = Pos.CENTER
            add(placeholderBox)
        }
        cellFactory = Callback<List, ListCell<Item>> {
            NotificationCell(store, context)
        }
    }
    internal val root = VBox().apply {
        alignment = Pos.CENTER
        add(listView)
        VBox.setVgrow(listView, Priority.ALWAYS)
    }
    private fun setAccount(account: Account): AccountNotifications {
        val accountNotification = accounts.computeIfAbsent(account.userId) {
            logger.debug { "creating view of notifications of ${account.userId}"}
            AccountNotifications(account, listView)
        }
        if (currentAccount == account.userId) return accountNotification
        logger.debug { "updating nodes of view of notifications of ${account.userId}"}
        placeholderBox.children.setAll(accountNotification.placeholderView.root)
        if (root.childrenUnmodifiable.size == 2) {
            root.children.set(0, accountNotification.fetchStatusView.root)
        } else {
            root.children.add(0, accountNotification.fetchStatusView.root)
        }
        currentAccount = account.userId
        return accountNotification
    }
    fun viewAccount(account: Account) {
        val notifications = setAccount(account)
        notifications.view()
        context.account = notifications
        listView.itemsProperty().set(notifications.list)
    }
}

private class ListContext(
        var account: AccountNotifications? = null
)

private class NotificationCell(
        private val store: AppStore,
        private val context: ListContext
): ListCell<Item>(), CoroutineScope by CoroutineScope(Dispatchers.Default) {
    private val timeView = DatatimeView()
    private val avatarView = AvatarView(userData = store.userData)
    private val senderLabel = Text()
    private val center = StackPane()
    private var server: Server? = null
    private var item: Item? = null
    private val menu by lazy {
        ContextMenu(Menu("Debug", null,
                MenuItem("Show popup").apply {
                    action { showPopup() }
                }
        ))
    }
    val root = HBox(4.0).apply {
        minWidth = 1.0
        prefWidth = 1.0
        background = whiteBackGround
        alignment = Pos.TOP_LEFT
        add(avatarView.root)

        vbox {
            spacing = 2.0
            HBox.setHgrow(this, Priority.ALWAYS)
            hbox(spacing = 10.0) {
                add(senderLabel)
                add(timeView.root)
            }
            add(center)
        }
        setOnContextMenuRequested {
            menu.show(this, null, it.x, it.y)
        }
    }
    private val messageView = MessageView(store.userData)
    private val fallbackCell = TextFlow().apply {
        // wrap text
        minWidth = 1.0
        prefWidth = 1.0
    }

    private val senderId = Channel<UserId>(Channel.CONFLATED)

    private fun showPopup() {
        val s = server ?: return
        val item = this.item ?: return
        popNotify(item, store, s)
    }
    override fun updateItem(item: Notification?, empty: Boolean) {
        super.updateItem(item, empty)
        if (empty || item == null) {
            graphic = null
            return
        }
        val event = item.event
        val content = event.content
        val account = context.account?: run {
            graphic = TextFlow(Text("type: ${event.type}, content: ${event.content}"))
            logger.error { "no server"}
            return
        }
        account.updateVisible(item)
        val s = account.account.server
        this.server = s
        this.item = item
        center.children.clear()
        senderId.offer(event.sender)
        timeView.updateTime(event.origin_server_ts)
        avatarView.updateUser(event.sender, s)
        senderLabel.fill = store.userData.getUserColor(event.sender)
        when {
            event.type == RoomEventType.Message -> if(content is NotificationResponse.Content.Message) {
                val msg = content.message
                messageView.update(msg, s, event.sender)
                messageView.node?.let {
                    center.children.add(it.node)
                }
            } else {
                logger.debug { "msg content $content"}
            }
            else -> {
                fallbackCell.children.setAll(Text("type: ${event.type}, content: ${event.content}"))
                center.children.add(fallbackCell)
            }
        }
        graphic = root
    }
    init {
        launch(Dispatchers.Main) {
            val newName = switchUpdates(senderId) { store.userData.getNameUpdates(it) }
            for (n in newName) {
                senderLabel.text = n
            }
        }
    }
}