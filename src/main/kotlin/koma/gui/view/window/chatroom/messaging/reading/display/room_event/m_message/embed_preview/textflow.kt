@file:Suppress("EXPERIMENTAL_API_USAGE")

package koma.gui.view.window.chatroom.messaging.reading.display.room_event.m_message.embed_preview

import javafx.scene.Node
import javafx.scene.text.Text
import javafx.scene.text.TextFlow
import koma.Server
import link.continuum.desktop.gui.list.user.UserDataStore

fun TextFlow.addStringWithElements(str: String, server: Server, data: UserDataStore) {
    val textelements = tokenize_string(str)
    val nodes = textelements.map { messageSliceView(it, server, data) }.toNodes()
    this.children.addAll(nodes)
}

/**
 * put larger elements on new lines
 */
fun List<FlowElement>.toNodes(): List<Node> {
    val nodes = mutableListOf<Node>()
    var prev = this.firstOrNull()
    for (f in this) {
        val pre = prev

        val textThenWeb = pre is InlineElement && f.isMultiLine() && !pre.endsWithNewline()
        val webThenText = pre?.isMultiLine() == true && f is InlineElement && !f.startsWithNewline()
        if (textThenWeb || webThenText) {
            nodes.add(Text("\n"))
        }
        nodes.add(f.node)

        prev = f
    }
    return nodes
}
