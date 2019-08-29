package koma.gui.element.control

// from http://dlsc.com/2017/09/07/javafx-tip-28-pretty-list-view/
import javafx.css.Stylesheet
import javafx.geometry.Orientation
import javafx.scene.control.ListView
import javafx.scene.control.ScrollBar

class PrettyListView<T> : ListView<T>() {

    private val vBar = ScrollBar()
    private val hBar = ScrollBar()

    init {

        skinProperty().addListener { _ ->
            // first bind, then add new scrollbars, otherwise the new bars will be found
            bindScrollBars()
            children.addAll(vBar, hBar)
        }
        styleClass.add("pretty-list-view")
        stylesheets.add("/css/prettylistview.css")

        vBar.isManaged = false
        vBar.orientation = Orientation.VERTICAL
        vBar.styleClass.add("pretty-scroll-bar")
        vBar.visibleProperty().bind(vBar.visibleAmountProperty().isNotEqualTo(0))

        hBar.isManaged = false
        hBar.orientation = Orientation.HORIZONTAL
        hBar.styleClass.add("pretty-scroll-bar")
        hBar.visibleProperty().bind(hBar.visibleAmountProperty().isNotEqualTo(0))
    }

    private fun bindScrollBars() {
        val nodes = lookupAll("VirtualScrollBar")
        for (node in nodes) {
            if (node is ScrollBar) {
                if (node.orientation == Orientation.VERTICAL) {
                    bindScrollBars(vBar, node)
                } else if (node.orientation == Orientation.HORIZONTAL) {
                    bindScrollBars(hBar, node)
                }
            }
        }
    }

    private fun bindScrollBars(scrollBarA: ScrollBar, scrollBarB: ScrollBar) {
        scrollBarA.valueProperty().bindBidirectional(scrollBarB.valueProperty())
        scrollBarA.minProperty().bindBidirectional(scrollBarB.minProperty())
        scrollBarA.maxProperty().bindBidirectional(scrollBarB.maxProperty())
        scrollBarA.visibleAmountProperty().bindBidirectional(scrollBarB.visibleAmountProperty())
        scrollBarA.unitIncrementProperty().bindBidirectional(scrollBarB.unitIncrementProperty())
        scrollBarA.blockIncrementProperty().bindBidirectional(scrollBarB.blockIncrementProperty())
    }

    override fun layoutChildren() {
        super.layoutChildren()

        val insets = insets
        val w = width
        val h = height
        val prefWidth = vBar.prefWidth(-1.0)
        vBar.resizeRelocate(w - prefWidth - insets.right, insets.top, prefWidth, h - insets.top - insets.bottom)

        val prefHeight = hBar.prefHeight(-1.0)
        hBar.resizeRelocate(insets.left, h - prefHeight - insets.bottom, w - insets.left - insets.right, prefHeight)
    }
}
