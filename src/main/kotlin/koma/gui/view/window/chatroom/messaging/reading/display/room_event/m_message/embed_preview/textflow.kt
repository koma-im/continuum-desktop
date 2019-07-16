package koma.gui.view.window.chatroom.messaging.reading.display.room_event.m_message.embed_preview

import javafx.scene.Node
import javafx.scene.text.Text
import javafx.scene.text.TextFlow
import koma.Koma
import tornadofx.*

fun TextFlow.addStringWithElements(str: String, koma: Koma) {
    val textelements = tokenize_string(str)
    val nodes = textelements.map { it.toFlow(koma) }.toNodes()
    this.addNodes(nodes)
}

fun TextFlow.addNodes(nodes: List<Node>) {
    nodes.forEach {  this.add(it) }
}

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
