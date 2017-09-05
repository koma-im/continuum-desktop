package view.popup

import com.moandjiezana.toml.Toml
import controller.events
import javafx.beans.binding.DoubleBinding
import javafx.event.EventHandler
import javafx.scene.Node
import javafx.scene.control.TabPane
import javafx.scene.control.TextField
import javafx.scene.control.Tooltip
import javafx.scene.image.Image
import javafx.scene.layout.GridPane
import org.controlsfx.control.PopOver
import tornadofx.*
import java.io.ByteArrayInputStream
import java.util.zip.ZipInputStream

/**
 * Created by developer on 2017/7/16.
 */

object EmojiData {
    val emojis: Map<String, Image>
    val categories: List<Pair<String, List<EmojiChar>>>

    init {
        println("start loading emojis")
        val uri = javaClass.getResourceAsStream("/emoji/png.zip");
        if (uri != null) {
            val zin = ZipInputStream(uri)
            var entry = zin.nextEntry
            val emojimap = HashMap<String, Image>()
            while (entry != null) {
                val code = entry.name.substringBefore(".png")
                val buffer = ByteArray(2048)
                val bytes = mutableListOf<Byte>()
                var len = zin.read(buffer)
                while (len > 0) {
                    bytes.addAll(buffer.take(len))
                    len = zin.read(buffer)
                }
                val ins = ByteArrayInputStream(bytes.toByteArray())
                val im = Image(ins)
                emojimap.set(code, im)
                entry = zin.nextEntry
            }
            emojis = emojimap
        } else {
            emojis = mapOf()
        }
        categories = getEmojiCategories()
    }

    fun getEmojiCategories(): List<Pair<String, List<EmojiChar>>> {
        val catread= javaClass.getResourceAsStream("/emoji/categories.toml")
        val toml = Toml().read( catread)
        val data: List<Map<String, Any>> = toml.getList("category")
        val categories = data.map {
            val emojis = it["emojis"] as List<Map<String, String>>
            Pair(
                    it["name"] as String,
                    emojis.map { EmojiChar(it["code"]!!, it["glyph"]!!, it["desc"]!!) }
            )
        }
        return categories
    }
}

class EmojiChar(
        val code: String,
        val glyph: String,
        val desc: String
) {
    fun image(): Image? {
        if (!EmojiData.emojis.containsKey(code))
            return null
        return EmojiData.emojis[this.code]
    }

    override fun toString() = glyph
}


class EmojiPanel(val input: TextField) : TabPane() {

    val pop = PopOver()

    init {
        events.beforeShutdownHook.add({ pop.hide() })
        pop.contentNode = this
        pop.arrowLocation = PopOver.ArrowLocation.TOP_CENTER
        pop.isDetachable = false
        this.tabMaxWidth = 10.0

        val emojisize = pop.widthProperty().subtract(45).divide(8)

        for ((kind: String, emojis: List<EmojiChar>) in EmojiData.categories) {
            tab() {
                this.isClosable = false
                this.tooltip = Tooltip(kind)
                this.graphic = imageview(emojis[0].image()!!) {
                    val emoiconsize = emojisize
                    this.fitWidthProperty().bind(emoiconsize)
                    this.fitHeightProperty().bind(emoiconsize)
                }
                scrollpane {
                    this.prefViewportWidth = 160.0
                    this.prefViewportHeight = 140.0
                    val gp = gridpane {
                        val validemojis= emojis.filter { it.image()!=null }.iterator()
                        putEmojisInRows(this, validemojis, emojisize)
                    }
                }
            }
        }
    }

    private fun putEmojisInRows(gp: GridPane, emojis: Iterator<EmojiChar>, emojisize: DoubleBinding) {
        while (emojis.hasNext()) {
            gp.row {
                for (ii in 0..7) {
                    if (!emojis.hasNext())
                        break
                    val emochar = emojis.next()
                    imageview(emochar.image()!!) {
                        tooltip(emochar.desc)
                        this.fitWidthProperty().bind(emojisize)
                        this.fitHeightProperty().bind(emojisize)
                        this.onMouseClicked = EventHandler {
                            println("emoji $emochar")
                            input.text += emochar.glyph
                            pop.hide()
                        }
                    }
                }
            }
        }
    }

    fun show(own: Node) {
        pop.show(own)
    }
}
