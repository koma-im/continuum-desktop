package koma.gui.view.window.chatroom.messaging.reading.display.room_event.m_message.embed_preview

import javafx.scene.Node
import javafx.scene.text.Text
import javafx.scene.text.TextFlow
import tornadofx.*

fun TextFlow.addStringWithElements(str: String) {
    val textelements =  StringElementTokenizer(str).elements
    val nodes = textelements.map { it.toFlow() }.toNodes()
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

        val textThenWeb = pre is InlineElement && f.isMultiLine() && pre.node.text.lastOrNull() != '\n'
        val webThenText = pre?.isMultiLine() == true && f is InlineElement && f.node.text.firstOrNull() != '\n'
        if (textThenWeb || webThenText) {
            nodes.add(Text("\n"))
        }
        nodes.add(f.node)

        prev = f
    }
    return nodes
}
