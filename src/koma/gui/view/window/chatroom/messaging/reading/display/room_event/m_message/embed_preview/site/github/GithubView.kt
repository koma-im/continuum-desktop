package koma.gui.view.window.chatroom.messaging.reading.display.room_event.m_message.embed_preview.site.github

import javafx.scene.control.MenuItem
import javafx.scene.layout.VBox
import koma.gui.view.window.chatroom.messaging.reading.display.ViewNode
import okhttp3.HttpUrl
import tornadofx.*

fun createGithubView(url: HttpUrl): GithubView? {
    val ps = url.pathSegments()
    val owner = ps.getOrNull(0) ?: return null
    val repo = ps.getOrNull(1) ?: return null
    return GithubView(owner, repo)
}

class GithubView(owner: String, repo: String): ViewNode {
    override val node = VBox()
    override val menuItems: List<MenuItem>

    init {
        with(node) {
            text("Github repo $repo")
            text("by $owner")
            text("preview coming soon")
        }

        val mi = MenuItem("sample menu item")
        menuItems = listOf(mi)
    }
}
