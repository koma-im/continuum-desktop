package koma.gui.element.control.skin

import javafx.beans.Observable
import javafx.beans.property.*
import javafx.beans.value.ChangeListener
import javafx.beans.value.ObservableValue
import javafx.collections.ObservableList
import javafx.event.Event
import javafx.event.EventDispatchChain
import javafx.event.EventHandler
import javafx.geometry.Orientation
import javafx.scene.AccessibleRole
import javafx.scene.Group
import javafx.scene.Node
import javafx.scene.Parent
import javafx.scene.control.Cell
import javafx.scene.control.IndexedCell
import javafx.scene.input.MouseEvent
import javafx.scene.input.ScrollEvent
import javafx.scene.layout.Region
import javafx.scene.shape.Rectangle
import javafx.util.Callback
import koma.gui.element.control.KVirtualScrollBar
import koma.gui.element.control.NullableIndexRange
import koma.gui.element.control.Utils
import mu.KotlinLogging
import java.util.*

private val logger = KotlinLogging.logger {}
/**
 * Implementation of a virtualized container using a cell based mechanism.
 */
class KVirtualFlow<I, T>(
        /**
         * For optimisation purposes, some use cases can trade dynamic cell length
         * for speed - if fixedCellSize is not null we'll use that rather
         * than determine it by querying the cell itself.
         */
        private val fixedCellSize: Double? = null
)
    : Region()
        where I :IndexedCell<T>{

    val visibleIndexRange = SimpleObjectProperty<NullableIndexRange>()
    fun visibleFirst(): T? {
        val i = firstVisibleCell?.item
        return i
    }
    /***************************************************************************
     * *
     * Private fields                                                          *
     * *
     */

    private var touchDetected = false
    private var mouseDown = false

    /**
     * The width of the VirtualFlow the last time it was laid out. We
     * use this information for several fast paths during the layout pass.
     */
    internal var lastWidth = -1.0

    /**
     * The height of the VirtualFlow the last time it was laid out. We
     * use this information for several fast paths during the layout pass.
     */
    internal var lastHeight = -1.0

    /**
     * The number of "virtual" cells in the flow the last time it was laid out.
     * For example, there may have been 1000 virtual cells, but only 20 actual
     * cells created and in use. In that case, lastCellCount would be 1000.
     */
    internal var lastCellCount = 0

    /**
     * The position last time we laid out. If none of the lastXXX vars have
     * changed respective to their values in layoutChildren, then we can just punt
     * out of the method (I hope...)
     */
    internal var lastPosition: Double = 1.0

    /**
     * The breadth of the first visible cell last time we laid out.
     */
    internal var lastCellBreadth = -1.0

    /**
     * The length of the first visible cell last time we laid out.
     */
    internal var lastCellLength = -1.0

    /**
     * The list of cells representing those cells which actually make up the
     * current view. The cells are ordered such that the first cell in this
     * list is the first in the view, and the last cell is the last in the
     * view. When pixel scrolling, the list is simply shifted and items drop
     * off the beginning or the end, depending on the order of scrolling.
     */
    internal val cells = ArrayLinkedList<I>()

    /**
     * A structure containing cells that can be reused later. These are cells
     * that at one time were needed to populate the view, but now are no longer
     * needed. We keep them here until they are needed again.
     */
    internal val pile = ArrayLinkedList<I>()

    /**
     * A special cell used to accumulate bounds, such that we reduce object
     * churn. This cell must be recreated whenever the cell factory function
     * changes.
     */
    internal var accumCell: I? = null

    /**
     * This group is used for holding the 'accumCell'. 'accumCell' must
     * be added to the skin for it to be styled. Otherwise, it doesn't
     * report the correct width/height leading to issues when scrolling
     * the flow
     */
    internal var accumCellParent: Group

    /**
     * The group which holds the cells.
     */
    internal val sheet: Group

    lateinit var sheetChildren: ObservableList<Node>

    /**
     * The scroll bar used to scrolling vertically.
     */
    internal val vbar = KVirtualScrollBar(this)

    /**
     * Control in which the cell's sheet is placed and forms the viewport. The
     * viewportBreadth and viewportLength are simply the dimensions of the
     * clipView.
     */
    internal var clipView: ClippedContainer

    // used for panning the virtual flow
    private var lastX: Double = 0.toDouble()
    private var lastY: Double = 0.toDouble()
    private var isPanning = false

    private var needsReconfigureCells = false // when cell contents are the same
    private var needsRecreateCells = false // when cell factory changed
    private var needsRebuildCells = false // when cell contents have changed
    private var needsCellsLayout = false
    private var sizeChanged = false
    private val dirtyCells = BitSet()

    /***************************************************************************
     * *
     * Properties                                                              *
     * *
     */

    /**
     * There are two main complicating factors in the implementation of the
     * VirtualFlow, which are made even more complicated due to the performance
     * sensitive nature of this code. The first factor is the actual
     * virtualization mechanism, wired together with the PositionMapper.
     * The second complicating factor is the desire to do minimal layout
     * and minimal updates to CSS.
     *
     * Since the layout mechanism runs at most once per pulse, we want to hook
     * into this mechanism for minimal recomputation. Whenever a layout pass
     * is run we record the width/height that the virtual flow was last laid
     * out to. In subsequent passes, if the width/height has not changed then
     * we know we only have to rebuild the cells. If the width or height has
     * changed, then we can make appropriate decisions based on whether the
     * width / height has been reduced or expanded.
     *
     * In various places, if requestLayout is called it is generally just
     * used to indicate that some form of layout needs to happen (either the
     * entire thing has to be reconstructed, or just the cells need to be
     * reconstructed, generally).
     *
     * The accumCell is a special cell which is used in some computations
     * when an actual cell for that item isn't currently available. However,
     * the accumCell must be cleared whenever the cellFactory function is
     * changed because we need to use the cells that come from the new factory.
     *
     * In addition to storing the lastWidth and lastHeight, we also store the
     * number of cells that existed last time we performed a layout. In this
     * way if the number of cells change, we can request a layout and when it
     * occurs we can tell that the number of cells has changed and react
     * accordingly.
     *
     * Because the VirtualFlow can be laid out horizontally or vertically a
     * naming problem is present when trying to conceptualize and implement
     * the flow. In particular, the words "width" and "height" are not
     * precise when describing the unit of measure along the "virtualized"
     * axis and the "orthogonal" axis. For example, the height of a cell when
     * the flow is vertical is the magnitude along the "virtualized axis",
     * and the width is along the axis orthogonal to it.
     *
     * Since "height" and "width" are not reliable terms, we use the words
     * "length" and "breadth" to describe the magnitude of a cell along
     * the virtualized axis and orthogonal axis. For example, in a vertical
     * flow, the height=length and the width=breadth. In a horizontal axis,
     * the height=breadth and the width=length.
     *
     * These terms are somewhat arbitrary, but chosen so that when reading
     * most of the below code you can think in just one dimension, with
     * helper functions converting width/height in to length/breadth, while
     * also being different from width/height so as not to get confused with
     * the actual width/height of a cell.
     */

    /**
     * Indicates whether the VirtualFlow viewport is capable of being panned
     * by the user (either via the mouse or touch events).
     */
    private val pannable = SimpleBooleanProperty(this, "pannable", true)
    var isPannable: Boolean
        get() = pannable.get()
        set(value) = pannable.set(value)

    // --- cell count
    /**
     * Indicates the number of cells that should be in the flow. The user of
     * the VirtualFlow must set this appropriately. When the cell count changes
     * the VirtualFlow responds by updating the visuals. If the items backing
     * the cells change, but the count has not changed, you must call the
     * reconfigureCells() function to update the visuals.
     */
    private val cellCount = object : SimpleIntegerProperty(this, "cellCount", 0) {
        private var oldCount = 0

        override fun invalidated() {
            val cellCount = get()

            val countChanged = oldCount != cellCount
            oldCount = cellCount

            // ensure that the virtual scrollbar adjusts in size based on the current
            // cell count.
            if (countChanged) {
                val lengthBar = vbar
                lengthBar.max = cellCount.toDouble()
            }

            // I decided *not* to reset maxPrefBreadth here for the following
            // situation. Suppose I have 30 cells and then I add 10 more. Just
            // because I added 10 more doesn't mean the max pref should be
            // reset. Suppose the first 3 cells were extra long, and I was
            // scrolled down such that they weren't visible. If I were to reset
            // maxPrefBreadth when subsequent cells were added or removed, then the
            // scroll bars would erroneously reset as well. So I do not reset
            // the maxPrefBreadth here.

            // Fix for RT-12512, RT-14301 and RT-14864.
            // Without this, the VirtualFlow length-wise scrollbar would not change
            // as expected. This would leave items unable to be shown, as they
            // would exist outside of the visible area, even when the scrollbar
            // was at its maximum position.
            // FIXME this should be only executed on the pulse, so this will likely
            // lead to performance degradation until it is handled properly.
            if (countChanged) {
                layoutChildren()

                // Fix for RT-13965: Without this line of code, the number of items in
                // the sheet would constantly grow, leaking memory for the life of the
                // application. This was especially apparent when the total number of
                // cells changes - regardless of whether it became bigger or smaller.
                sheetChildren.clear()

                val parent = parent
                parent?.requestLayout()
            }
            // TODO suppose I had 100 cells and I added 100 more. Further
            // suppose I was scrolled to the bottom when that happened. I
            // actually want to update the position of the mapper such that
            // the view remains "stable".
        }
    }


    // --- position
    /**
     * The position of the VirtualFlow within its list of cells. This is a value
     * between 0 and 1.
     */
    private val position = object : SimpleDoubleProperty(this, "position") {
        override fun setValue(v: Number?) {
            super.setValue(Utils.clamp(0.0, get(), 1.0))
        }

        override fun invalidated() {
            super.invalidated()
            requestLayout()
        }
    }

    // --- fixed cell size




    // --- Cell Factory
    private var cellFactory: ObjectProperty<Callback<KVirtualFlow<I, T>, I>>? = null

    /**
     * Locates and returns the last non-empty IndexedCell that is currently
     * partially or completely visible. This function may return null if there
     * are no cells, or if the viewport length is 0.
     * @return the last visible cell
     */
    val lastVisibleCell: I?
        get() {
            if (cells.isEmpty() || viewportLength <= 0) return null

            var cell: I?
            for (i in cells.indices.reversed()) {
                cell = cells[i]
                if (!cell!!.isEmpty) {
                    return cell
                }
            }

            return null
        }

    /**
     * Locates and returns the first non-empty IndexedCell that is partially or
     * completely visible. This really only ever returns null if there are no
     * cells or the viewport length is 0.
     * @return the first visible cell
     */
    val firstVisibleCell: I?
        get() {
            if (cells.isEmpty()) {
                logger.trace { "there are no cells" }
                return null
            }
            if ( viewportLength <= 0) {
                logger.debug { "the viewport length is 0." }
                return null
            }
            val cell = cells.first
            return if (cell!!.isEmpty) {
                logger.trace { "cells.first isEmpty" }
                null
            } else cell
        }

    /**
     * The maximum preferred size in the non-virtual direction. For example,
     * if vertical, then this is the max pref width of all cells encountered.
     *
     * In general, this is the largest preferred size in the non-virtual
     * direction that we have ever encountered. We don't reduce this size
     * unless instructed to do so, so as to reduce the amount of scroll bar
     * jitter.
     */
    internal var maxPrefBreadth: Double = 0.toDouble()
        private set

    /**
     * The breadth of the viewport portion of the VirtualFlow as computed during
     * the layout pass. In a vertical flow this would be the same as the clip
     * view width. In a horizontal flow this is the clip view height.
     */
    private var viewportBreadth: Double = 0.toDouble()

    /**
     * The length of the viewport portion of the VirtualFlow as computed
     * during the layout pass. In a vertical flow this would be the same as the
     * clip view height. In a horizontal flow this is the clip view width.
     */
    internal var viewportLength: Double = 0.toDouble()

    // Returns last visible cell whose bounds are entirely within the viewport
    // we use the magic +2 to allow for a little bit of fuzziness,
    // this is to help in situations such as RT-34407
    internal val lastVisibleCellWithinViewPort: I?
        get() {
            if (cells.isEmpty() || viewportLength <= 0) return null

            var cell: I?
            val max = viewportLength
            for (i in cells.indices.reversed()) {
                cell = cells[i]
                if (cell!!.isEmpty) continue

                val cellStart = getCellPosition(cell)
                val cellEnd = cellStart + getCellLength(cell)
                if (cellEnd <= max + 2) {
                    return cell
                }
            }

            return null
        }

    // Returns first visible cell whose bounds are entirely within the viewport
    internal val firstVisibleCellWithinViewPort: I?
        get() {
            if (cells.isEmpty() || viewportLength <= 0) return null

            var cell: I?
            for (i in cells.indices) {
                cell = cells[i]
                if (cell!!.isEmpty) continue

                val cellStart = getCellPosition(cell)
                if (cellStart >= 0) {
                    return cell
                }
            }

            return null
        }

    private val privateCells = ArrayList<I>()

    private val prefLength: Double
        get() {
            var sum = 0.0
            val rows = Math.min(10, getCellCount())
            for (i in 0 until rows) {
                sum += getCellLength(i)
            }
            return sum
        }
    /***************************************************************************
     * *
     * Constructors                                                            *
     * *
     */

    /**
     * Creates a new VirtualFlow instance.
     */
    init {
        styleClass.add("virtual-flow")
        id = "virtual-flow"

        // initContent
        // --- sheet
        sheet = Group()
        sheet.styleClass.add("sheet")
        sheet.isAutoSizeChildren = false

        sheetChildren = sheet.children

        // --- clipView
        clipView = ClippedContainer(this)
        clipView.node = sheet
        children.add(clipView)

        // --- accumCellParent
        accumCellParent = Group()
        accumCellParent.isVisible = false
        children.add(accumCellParent)


        /*
        ** don't allow the ScrollBar to handle the ScrollEvent,
        ** In a VirtualFlow a vertical scroll should scroll on the vertical only,
        ** whereas in a horizontal ScrollBar it can scroll horizontally.
        */
        // block the event from being passed down to children
        val blockEventDispatcher = { event: Event, _: EventDispatchChain -> event }
        // block ScrollEvent from being passed down to scrollbar's skin
        val oldVsbEventDispatcher = vbar.eventDispatcher
        vbar.setEventDispatcher { event, t ->
            var tail = t
            if (event.eventType == ScrollEvent.SCROLL && !(event as ScrollEvent).isDirect) {
                tail = tail.prepend(blockEventDispatcher)
                tail = tail.prepend(oldVsbEventDispatcher)
                return@setEventDispatcher tail . dispatchEvent (event)
            }
            oldVsbEventDispatcher.dispatchEvent(event, tail)
        }
        /*
        ** listen for ScrollEvents over the whole of the VirtualFlow
        ** area, the above dispatcher having removed the ScrollBars
        ** scroll event handling.
        */
        onScroll = EventHandler { event ->
            /**
             * calculate the delta in the direction of the flow.
             */
            var virtualDelta = 0.0
            @Suppress("WHEN_ENUM_CAN_BE_NULL_IN_JAVA")
            when (event.textDeltaYUnits) {
                ScrollEvent.VerticalTextScrollUnits.PAGES -> virtualDelta = event.textDeltaY * lastHeight
                ScrollEvent.VerticalTextScrollUnits.LINES -> {
                    var lineSize: Double
                    lineSize = if (fixedCellSize != null) {
                        fixedCellSize
                    } else {
                        // For the scrolling to be reasonably consistent
                        // we set the lineSize to the average size
                        // of all currently loaded lines.
                        val lastCell = cells.last
                        (getCellPosition(lastCell) + getCellLength(lastCell) - getCellPosition(cells.first)) / cells.size
                    }

                    if (lastHeight / lineSize < MIN_SCROLLING_LINES_PER_PAGE) {
                        lineSize = lastHeight / MIN_SCROLLING_LINES_PER_PAGE
                    }

                    virtualDelta = event.textDeltaY * lineSize
                }
                ScrollEvent.VerticalTextScrollUnits.NONE -> virtualDelta = event.deltaY
            }

            if (virtualDelta != 0.0) {
                /**
                 ** only consume it if we use it
                 */
                val result = scrollPixels(-virtualDelta)
                if (result != 0.0) {
                    event.consume()
                }
            }
        }


        addEventFilter(MouseEvent.MOUSE_PRESSED) { e ->
            mouseDown = true
            if (isFocusTraversable) {
                // We check here to see if the current focus owner is within
                // this VirtualFlow, and if so we back-off from requesting
                // focus back to the VirtualFlow itself. This is particularly
                // relevant given the bug identified in RT-32869. In this
                // particular case TextInputControl was clearing selection
                // when the focus on the TextField changed, meaning that the
                // right-click context menu was not showing the correct
                // options as there was no selection in the TextField.
                var doFocusRequest = true
                val focusOwner = scene.focusOwner
                if (focusOwner != null) {
                    var parent: Parent? = focusOwner.parent
                    while (parent != null) {
                        if (parent == this@KVirtualFlow) {
                            doFocusRequest = false
                            break
                        }
                        parent = parent.parent
                    }
                }

                if (doFocusRequest) {
                    requestFocus()
                }
            }

            lastX = e.x
            lastY = e.y

            // determine whether the user has push down on the virtual flow,
            // or whether it is the scrollbar. This is done to prevent
            // mouse events being 'doubled up' when dragging the scrollbar
            // thumb - it has the side-effect of also starting the panning
            // code, leading to flicker
            isPanning = !(vbar.boundsInParent.contains(e.x, e.y))
        }
        addEventFilter(MouseEvent.MOUSE_RELEASED) { _ ->
            mouseDown = false
        }
        addEventFilter(MouseEvent.MOUSE_DRAGGED) { e ->
            if (!isPanning || !isPannable) return@addEventFilter

            // With panning enabled, we support panning in both vertical
            // and horizontal directions, regardless of the fact that
            // VirtualFlow is virtual in only one direction.
            val yDelta = lastY - e.y

            // figure out the distance that the mouse moved in the virtual
            // direction, and then perform the movement along that axis
            // virtualDelta will contain the amount we actually did move
            val virtualDelta =  yDelta
            val actual = scrollPixels(virtualDelta)
            if (actual != 0.0) {
                // update last* here, as we know we've just adjusted the
                // scrollbar. This means we don't get the situation where a
                // user presses-and-drags a long way past the min or max
                // values, only to change directions and see the scrollbar
                // start moving immediately.
                    lastY = e.y
            }
        }

        /*
         * We place the scrollbars _above_ the rectangle, such that the drag
         * operations often used in conjunction with scrollbars aren't
         * misinterpreted as drag operations on the rectangle as well (which
         * would be the case if the scrollbars were underneath it as the
         * rectangle itself doesn't block the mouse.
         */
        // --- vbar
        vbar.orientation = Orientation.VERTICAL
        vbar.addEventHandler(MouseEvent.ANY) { event -> event.consume() }
        children.add(vbar)


        // initBinds
        // clipView binds

        val listenerY = object : ChangeListener<Number> {
            override fun changed(ov: ObservableValue<out Number>?, t: Number?, t1: Number?) {
                clipView.setClipY(if (true) 0.0 else vbar.value)
            }
        }
        vbar.valueProperty().addListener(listenerY)

        super.heightProperty().addListener { _, oldHeight, newHeight ->
            // Fix for RT-8480, where the VirtualFlow does not show its content
            // after changing size to 0 and back.
            if (oldHeight.toDouble() == 0.0 && newHeight.toDouble() > 0) {
                recreateCells()
            }
        }


        /*
        ** there are certain animations that need to know if the touch is
        ** happening.....
        */
        setOnTouchPressed { _ ->
            touchDetected = true
        }

        setOnTouchReleased { _ ->
            touchDetected = false
        }
        setPosition(1.0)
    }

    fun getCellCount(): Int {
        return cellCount.get()
    }

    fun setCellCount(value: Int) {
        cellCount.set(value)
    }

    fun getPosition(): Double {
        return position.get()
    }

    fun setPosition(value: Double) {
        position.set(value)
    }

    /**
     * Sets a new cell factory to use in the VirtualFlow. This forces all old
     * cells to be thrown away, and new cells to be created with
     * the new cell factory.
     * @param value the new cell factory
     */
    fun setCellFactory(value: Callback<KVirtualFlow<I, T>, I>) {
        cellFactoryProperty().set(value)
    }

    /**
     * Returns the current cell factory.
     * @return the current cell factory
     */
    fun getCellFactory(): Callback<KVirtualFlow<I, T>, I>? {
        return if (cellFactory == null) null else cellFactory!!.get()
    }

    /**
     *
     * Setting a custom cell factory has the effect of deferring all cell
     * creation, allowing for total customization of the cell. Internally, the
     * VirtualFlow is responsible for reusing cells - all that is necessary
     * is for the custom cell factory to return from this function a cell
     * which might be usable for representing any item in the VirtualFlow.
     *
     *
     * Refer to the [Cell] class documentation for more detail.
     * @return  the cell factory property
     */
    fun cellFactoryProperty(): ObjectProperty<Callback<KVirtualFlow<I, T>, I>> {
        if (cellFactory == null) {
            cellFactory = object : SimpleObjectProperty<Callback<KVirtualFlow<I, T>, I>>(this, "cellFactory") {
                override fun invalidated() {
                    if (get() != null) {
                        accumCell = null
                        isNeedsLayout = true
                        recreateCells()
                        if (parent != null) parent.requestLayout()
                    }
                }
            }
        }
        return cellFactory!!
    }

    private fun updateVisibleIndices() {
            visibleIndexRange.set(NullableIndexRange(
                    firstVisibleCell?.index,
                    lastVisibleCell?.index))
    }

    fun scroll(r: Float) {
        scrollPixels(r * lastHeight)
    }

    override fun layoutChildren() {
        if (needsRecreateCells) {
            lastWidth = -1.0
            lastHeight = -1.0
            releaseCell(accumCell)
            sheet.children.clear()
            cells.forEach { it.updateIndex(-1) }
            cells.clear()
            pile.clear()
            releaseAllPrivateCells()
        } else if (needsRebuildCells) {
            lastWidth = -1.0
            lastHeight = -1.0
            releaseCell(accumCell)
            cells.forEach { it.updateIndex(-1) }
            addAllToPile()
            releaseAllPrivateCells()
        } else if (needsReconfigureCells) {
            maxPrefBreadth = -1.0
            lastWidth = -1.0
            lastHeight = -1.0
        }

        if (!dirtyCells.isEmpty) {
            var index: Int
            val cellsSize = cells.size
            index = dirtyCells.nextSetBit(0)
            while (index != -1 && index < cellsSize) {
                val cell = cells[index]
                cell?.requestLayout()
                dirtyCells.clear(index)
                index = dirtyCells.nextSetBit(0)
            }

            maxPrefBreadth = -1.0
            lastWidth = -1.0
            lastHeight = -1.0
        }

        val hasSizeChange = sizeChanged
        var recreatedOrRebuilt = needsRebuildCells || needsRecreateCells || sizeChanged

        needsRecreateCells = false
        needsReconfigureCells = false
        needsRebuildCells = false
        sizeChanged = false

        if (needsCellsLayout) {
            cells.forEach { it?.requestLayout() }
            needsCellsLayout = false

            // yes, we return here - if needsCellsLayout was set to true, we
            // only did it to do the above - not rerun the entire layout.
            return
        }

        val width = width
        val height = height
        val position = getPosition()

        // if the width and/or height is 0, then there is no point doing
        // any of this work. In particular, this can happen during startup
        if (width <= 0 || height <= 0) {
            addAllToPile()
            lastWidth = width
            lastHeight = height
            vbar.isVisible = false
            return
        }

        // we check if any of the cells in the cells list need layout. This is a
        // sign that they are perhaps animating their sizes. Without this check,
        // we may not perform a layout here, meaning that the cell will likely
        // 'jump' (in height normally) when the user drags the virtual thumb as
        // that is the first time the layout would occur otherwise.
        var cellNeedsLayout = false

        cellNeedsLayout = cellNeedsLayout || cells.any { it.isNeedsLayout }

        val cellCount = getCellCount()
        val firstCell = firstVisibleCell
        val lastCell = lastVisibleCell

        // If no cells need layout, we check other criteria to see if this
        // layout call is even necessary. If it is found that no layout is
        // needed, we just punt.
        if (!cellNeedsLayout) {
            var cellSizeChanged = false
            if (firstCell != null) {
                val breadth = getCellBreadth(firstCell)
                val length = getCellLength(firstCell)
                cellSizeChanged = breadth != lastCellBreadth || length != lastCellLength
                lastCellBreadth = breadth
                lastCellLength = length
            }

            if (width == lastWidth &&
                    height == lastHeight &&
                    cellCount == lastCellCount  &&
                    position == lastPosition &&
                    !cellSizeChanged) {
                // TODO this happens to work around the problem tested by
                // testCellLayout_LayoutWithoutChangingThingsUsesCellsInSameOrderAsBefore
                // but isn't a proper solution. Really what we need to do is, when
                // laying out cells, we need to make sure that if a cell is pressed
                // AND we are doing a full rebuild then we need to make sure we
                // use that cell in the same physical location as before so that
                // it gets the mouse release event.
                return
            }
        }

        /*
         * This function may get called under a variety of circumstances.
         * It will determine what has changed from the last time it was laid
         * out, and will then take one of several execution paths based on
         * what has changed so as to perform minimal layout work and also to
         * give the expected behavior. One or more of the following may have
         * happened:
         *
         *  1) width/height has changed
         *      - If the width and/or height has been reduced (but neither of
         *        them has been expanded), then we simply have to reposition and
         *        resize the scroll bars
         *      - If the width (in the vertical case) has expanded, then we
         *        need to resize the existing cells and reposition and resize
         *        the scroll bars
         *      - If the height (in the vertical case) has expanded, then we
         *        need to resize and reposition the scroll bars and add
         *        any trailing cells
         *
         *  2) cell count has changed
         *      - If the number of cells is bigger, or it is smaller but not
         *        so small as to move the position then we can just update the
         *        cells in place without performing layout and update the
         *        scroll bars.
         *      - If the number of cells has been reduced and it affects the
         *        position, then move the position and rebuild all the cells
         *        and update the scroll bars
         *
         *  3) size of the cell has changed
         *      - If the size changed in the virtual direction (ie: height
         *        in the case of vertical) then layout the cells, adding
         *        trailing cells as necessary and updating the scroll bars
         *      - If the size changed in the non virtual direction (ie: width
         *        in the case of vertical) then simply adjust the widths of
         *        the cells as appropriate and adjust the scroll bars
         *
         *  4) vertical changed, cells is empty, maxPrefBreadth == -1, etc
         *      - Full rebuild.
         *
         * Each of the conditions really resolves to several of a handful of
         * possible outcomes:
         *  a) reposition & rebuild scroll bars
         *  b) resize cells in non-virtual direction
         *  c) add trailing cells
         *  d) update cells
         *  e) resize cells in the virtual direction
         *  f) all of the above
         *
         * So this function first determines what outcomes need to occur, and
         * then will execute all the ones that really need to happen. Every code
         * path ends up touching the "reposition & rebuild scroll bars" outcome,
         * so that one will be executed every time.
         */
        var needTrailingCells = false
        var rebuild = cellNeedsLayout ||
                cells.isEmpty() ||
                maxPrefBreadth == -1.0 ||
                position != lastPosition ||
                cellCount != lastCellCount ||
                hasSizeChange || height < lastHeight

        if (!rebuild) {
            // Check if maxPrefBreadth didn't change
            val maxPrefBreadth = maxPrefBreadth
            var foundMax = false
            for (i in cells.indices) {
                val breadth = getCellBreadth(cells[i])
                if (maxPrefBreadth == breadth) {
                    foundMax = true
                } else if (breadth > maxPrefBreadth) {
                    rebuild = true
                    break
                }
            }
            if (!foundMax) { // All values were lower
                rebuild = true
            }
        }

        if (!rebuild) {
            if (true && height > lastHeight || !true && width > lastWidth) {
                // resized in the virtual direction
                needTrailingCells = true
            }
        }

        initViewport()

        // Get the index of the "current" cell
        var currentIndex = computeCurrentIndex()
        if (lastCellCount != cellCount) {
            // The cell count has changed. We want to keep the viewport
            // stable if possible. If position was 0 or 1, we want to keep
            // the position in the same place. If the new cell count is >=
            // the currentIndex, then we will adjust the position to be 1.
            // Otherwise, our goal is to leave the index of the cell at the
            // bottom consistent, with the same translation etc.
            if (position == 0.0 || position == 1.0) {
                // Update the item count
                //                setItemCount(cellCount);
            } else if (currentIndex >= cellCount) {
                setPosition(1.0)
                //                setItemCount(cellCount);
            } else if (lastCell != null) {
                getCellPosition(lastCell)
                val lastCellIndex = getCellIndex(lastCell)
                adjustPositionToIndex(lastCellIndex)
                -computeOffsetForCell(lastCellIndex)
                // ToDO figure out whether or how to adjust
                // adjustByPixelAmount(viewportTopToCellTop - lastCellOffset)
            }

            // Update the current index
            currentIndex = computeCurrentIndex()
        }

        if (rebuild) {
            maxPrefBreadth = -1.0
            // Start by dumping all the cells into the pile
            addAllToPile()

            // The distance from the top of the viewport to the top of the
            // cell for the current index.
            val offset = -computeViewportOffset(getPosition())

            // Add all the leading and trailing cells (the call to add leading
            // cells will add the current cell as well -- that is, the one that
            // represents the current position on the mapper).
            addLeadingCells(currentIndex, offset)

            // Force filling of space with empty cells if necessary
            addTrailingCells(true)
        } else if (needTrailingCells) {
            addTrailingCells(true)
        }

        recreatedOrRebuilt = recreatedOrRebuilt || rebuild
        updateScrollBarsAndCells(recreatedOrRebuilt)

        lastWidth = getWidth()
        lastHeight = getHeight()
        lastCellCount = getCellCount()
        lastPosition = getPosition()

        cleanPile()
    }

    override fun setWidth(value: Double) {
        if (value != lastWidth) {
            super.setWidth(value)
            sizeChanged = true
            isNeedsLayout = true
            requestLayout()
        }
    }

    override fun setHeight(value: Double) {
        if (value != lastHeight) {
            super.setHeight(value)
            sizeChanged = true
            isNeedsLayout = true
            requestLayout()
        }
    }

    /**
     * Get a cell which can be used in the layout. This function will reuse
     * cells from the pile where possible, and will create new cells when
     * necessary.
     * @param prefIndex the preferred index
     * @return the available cell
     */
    protected fun getAvailableCell(prefIndex: Int): I {
        var cell: I? = null

        // Fix for RT-12822. We try to retrieve the cell from the pile rather
        // than just grab a random cell from the pile (or create another cell).
        var i = 0
        val max = pile.size
        while (i < max) {
            val _cell = pile[i]!!

            if (getCellIndex(_cell) == prefIndex) {
                cell = _cell
                pile.removeAt(i)
                break
            }
            i++
        }

        if (cell == null && !pile.isEmpty()) {
            cell = pile.removeLast()
        }

        if (cell == null) {
            cell = getCellFactory()!!.call(this)
            cell!!.properties[NEW_CELL] = null
        }

        if (cell.parent == null) {
            sheetChildren.add(cell)
        }

        return cell
    }

    /**
     * This method will remove all cells from the VirtualFlow and remove them,
     * adding them to the 'pile' (that is, a place from where cells can be used
     * at a later date). This method is protected to allow subclasses to clean up
     * appropriately.
     */
    protected fun addAllToPile() {
        var i = 0
        val max = cells.size
        while (i < max) {
            addToPile(cells.removeFirst()!!)
            i++
        }
    }

    /**
     * Gets a cell for the given index if the cell has been created and laid out.
     * "Visible" is a bit of a misnomer, the cell might not be visible in the
     * viewport (it may be clipped), but does distinguish between cells that
     * have been created and are in use vs. those that are in the pile or
     * not created.
     * @param index the index
     * @return the visible cell
     */
    fun getVisibleCell(index: Int): I? {
        if (cells.isEmpty()) return null

        // check the last index
        val lastCell = cells.last
        val lastIndex = getCellIndex(lastCell)
        if (index == lastIndex) return lastCell

        // check the first index
        val firstCell = cells.first
        val firstIndex = getCellIndex(firstCell)
        if (index == firstIndex) return firstCell

        // if index is > firstIndex and < lastIndex then we can get the index
        if (index > firstIndex && index < lastIndex) {
            val cell = cells[index - firstIndex]
            if (getCellIndex(cell) == index) return cell
        }

        // there is no visible cell for the specified index
        return null
    }

    /**
     * Adjust the position of cells so that the specified cell
     * will be positioned at the start of the viewport. The given cell must
     * already be "live".
     * @param firstCell the first cell
     */
    fun scrollToTop(firstCell: I?) {
        if (firstCell != null) {
            scrollPixels(getCellPosition(firstCell))
        }
    }

    /**
     * Adjust the position of cells so that the specified cell
     * will be positioned at the end of the viewport. The given cell must
     * already be "live".
     * @param lastCell the last cell
     */
    fun scrollToBottom(lastCell: I?) {
        if (lastCell != null) {
            scrollPixels(getCellPosition(lastCell) + getCellLength(lastCell) - viewportLength)
        }
    }

    /**
     * Adjusts the cells such that the selected cell will be fully visible in
     * the viewport (but only just).
     * @param cell the cell
     */
    fun scrollTo(cell: I?) {
        if (cell != null) {
            val start = getCellPosition(cell)
            val length = getCellLength(cell)
            val end = start + length
            val viewportLength = viewportLength

            if (start < 0) {
                scrollPixels(start)
            } else if (end > viewportLength) {
                scrollPixels(end - viewportLength)
            }
        }
    }

    /**
     * Adjusts the cells such that the cell in the given index will be fully visible in
     * the viewport.
     * @param index the index
     */
    fun scrollTo(index: Int) {
        val cell = getVisibleCell(index)
        if (cell != null) {
            scrollTo(cell)
        } else {
            return
        }
    }

    /**
     * Adjusts the cells such that the cell in the given index will be fully visible in
     * the viewport, and positioned at the very top of the viewport.
     * @param index the index
     */
    fun scrollToTop(index: Int) {
        var posSet = false

        if (index >= getCellCount() - 1) {
            setPosition(1.0)
            posSet = true
        } else if (index < 0) {
            setPosition(0.0)
            posSet = true
        }

        if (!posSet) {
            adjustPositionToIndex(index)
            val offset = -computeOffsetForCell(index)
            adjustByPixelAmount(offset)
        }

        requestLayout()
    }

    //    //TODO We assume all the cell have the same length.  We will need to support
    //    // cells of different lengths.
    //    public void scrollToOffset(int offset) {
    //        scrollPixels(offset * getCellLength(0));
    //    }

    /**
     * Given a delta value representing a number of pixels, this method attempts
     * to move the VirtualFlow in the given direction (positive is down/right,
     * negative is up/left) the given number of pixels. It returns the number of
     * pixels actually moved.
     * @param delta the delta value
     * @return the number of pixels actually moved
     */
    fun scrollPixels(delta: Double): Double {
        // Short cut this method for cases where nothing should be done
        if (delta == 0.0) return 0.0

        val pos = getPosition()
        if (pos == 0.0 && delta < 0) return 0.0
        if (pos == 1.0 && delta > 0) return 0.0

        adjustByPixelAmount(delta)
        if (pos == getPosition()) {
            // The pos hasn't changed, there's nothing to do. This is likely
            // to occur when we hit either extremity
            return 0.0
        }

        // Now move stuff around. Translating by pixels fundamentally means
        // moving the cells by the delta. However, after having
        // done that, we need to go through the cells and see which cells,
        // after adding in the translation factor, now fall off the viewport.
        // Also, we need to add cells as appropriate to the end (or beginning,
        // depending on the direction of travel).
        //
        // One simplifying assumption (that had better be true!) is that we
        // will only make it this far in the function if the virtual scroll
        // bar is visible. Otherwise, we never will pixel scroll. So as we go,
        // if we find that the maxPrefBreadth exceeds the viewportBreadth,
        // then we will be sure to show the breadthBar and update it
        // accordingly.
        if (cells.size > 0) {
            for (i in cells.indices) {
                val cell = cells[i]!!
                positionCell(cell, getCellPosition(cell) - delta)
            }

            // Fix for RT-32908
            var firstCell = cells.first
            var layoutY = if (firstCell == null) 0.0 else getCellPosition(firstCell)
            for (i in cells.indices) {
                val cell = cells[i]!!
                val actualLayoutY = getCellPosition(cell)
                if (Math.abs(actualLayoutY - layoutY) > 0.001) {
                    // we need to shift the cell to layoutY
                    positionCell(cell, layoutY)
                }

                layoutY += getCellLength(cell)
            }
            // end of fix for RT-32908
            cull()
            firstCell = cells.first

            // Add any necessary leading cells
            if (firstCell != null) {
                val firstIndex = getCellIndex(firstCell)
                val prevIndexSize = getCellLength(firstIndex - 1)
                addLeadingCells(firstIndex - 1, getCellPosition(firstCell) - prevIndexSize)
            } else {
                val currentIndex = computeCurrentIndex()

                // The distance from the top of the viewport to the top of the
                // cell for the current index.
                val offset = -computeViewportOffset(getPosition())

                // Add all the leading and trailing cells (the call to add leading
                // cells will add the current cell as well -- that is, the one that
                // represents the current position on the mapper).
                addLeadingCells(currentIndex, offset)
            }

            // Starting at the tail of the list, loop adding cells until
            // all the space on the table is filled up. We want to make
            // sure that we DO NOT add empty trailing cells (since we are
            // in the full virtual case and so there are no trailing empty
            // cells).
            if (!addTrailingCells(false)) {
                // Reached the end, but not enough cells to fill up to
                // the end. So, remove the trailing empty space, and translate
                // the cells down
                val lastCell = lastVisibleCell
                val lastCellSize = getCellLength(lastCell)
                val cellEnd = getCellPosition(lastCell) + lastCellSize
                val viewportLength = viewportLength

                if (cellEnd < viewportLength) {
                    // Reposition the nodes
                    val emptySize = viewportLength - cellEnd
                    for (i in cells.indices) {
                        val cell = cells[i]
                        positionCell(cell, getCellPosition(cell) + emptySize)
                    }
                    setPosition(1.0)
                    // fill the leading empty space
                    firstCell = cells.first
                    val firstIndex = getCellIndex(firstCell)
                    val prevIndexSize = getCellLength(firstIndex - 1)
                    addLeadingCells(firstIndex - 1, getCellPosition(firstCell) - prevIndexSize)
                }
            }
        }

        // Now throw away any cells that don't fit
        cull()

        // Finally, update the scroll bars
        updateScrollBarsAndCells(false)
        lastPosition = getPosition()

        // notify
        updateVisibleIndices()
        return delta // TODO fake
    }

    override fun computePrefWidth(height: Double): Double {
        val w = getPrefBreadth(height)
        return w + vbar.prefWidth(-1.0)
    }

    override fun computePrefHeight(width: Double): Double {
        val h = prefLength
        return h
    }

    /**
     * Return a cell for the given index. This may be called for any cell,
     * including beyond the range defined by cellCount, in which case an
     * empty cell will be returned. The returned value should not be stored for
     * any reason.
     * @param index the index
     * @return the cell
     */
    fun getCell(index: Int): I? {
        // If there are cells, then we will attempt to get an existing cell
        if (!cells.isEmpty()) {
            // First check the cells that have already been created and are
            // in use. If this call returns a value, then we can use it
            val cell = getVisibleCell(index)
            if (cell != null) return cell
        }

        // check the pile
        for (i in pile.indices) {
            val cell = pile[i]
            if (getCellIndex(cell) == index) {
                // Note that we don't remove from the pile: if we do it leads
                // to a severe performance decrease. This seems to be OK, as
                // getCell() is only used for cell measurement purposes.
                // pile.remove(i);
                return cell
            }
        }

        if (pile.size > 0) {
            return pile[0]
        }

        // We need to use the accumCell and return that
        if (accumCell == null) {
            val cellFactory = getCellFactory()
            if (cellFactory != null) {
                accumCell = cellFactory.call(this)
                accumCell!!.properties[NEW_CELL] = null
                accumCellParent.children.setAll(accumCell)

                // Note the screen reader will attempt to find all
                // the items inside the view to calculate the item count.
                // Having items under different parents (sheet and accumCellParent)
                // leads the screen reader to compute wrong values.
                // The regular scheme to provide items to the screen reader
                // uses getPrivateCell(), which places the item in the sheet.
                // The accumCell, and its children, should be ignored by the
                // screen reader.
                accumCell!!.accessibleRole = AccessibleRole.NODE
                accumCell!!.childrenUnmodifiable.addListener { _: Observable ->
                    for (n in accumCell!!.childrenUnmodifiable) {
                        n.accessibleRole = AccessibleRole.NODE
                    }
                }
            }
        }
        setCellIndex(accumCell, index)
        resizeCellSize(accumCell)
        return accumCell
    }

    /**
     * The VirtualFlow uses this method to set a cells index (rather than calling
     * [IndexedCell.updateIndex] directly), so it is a perfect place
     * for subclasses to override if this if of interest.
     *
     * @param cell The cell whose index will be updated.
     * @param index The new index for the cell.
     */
    protected fun setCellIndex(cell: I?, index: Int) {
        assert(cell != null)

        cell!!.updateIndex(index)

        // make sure the cell is sized correctly. This is important for both
        // general layout of cells in a VirtualFlow, but also in cases such as
        // RT-34333, where the sizes were being reported incorrectly to the
        // ComboBox popup.
        if (cell.isNeedsLayout && cell.scene != null || cell.properties.containsKey(NEW_CELL)) {
            cell.applyCss()
            cell.properties.remove(NEW_CELL)
        }
    }

    /**
     * Return the index for a given cell. This allows subclasses to customise
     * how cell indices are retrieved.
     * @param cell the cell
     * @return the index
     */
    protected fun getCellIndex(cell: I?): Int {
        return cell!!.index
    }

    /**
     * Compute and return the length of the cell for the given index. This is
     * called both internally when adjusting by pixels, and also at times
     * by PositionMapper (see the getItemSize callback). When called by
     * PositionMapper, it is possible that it will be called for some index
     * which is not associated with any cell, so we have to do a bit of work
     * to use a cell as a helper for computing cell size in some cases.
     */
    internal fun getCellLength(index: Int): Double {
        if (fixedCellSize != null) return fixedCellSize

        val cell = getCell(index)
        val length = getCellLength(cell)
        releaseCell(cell)
        return length
    }

    /**
     */
    internal fun getCellBreadth(index: Int): Double {
        val cell = getCell(index)
        val b = getCellBreadth(cell)
        releaseCell(cell)
        return b
    }

    /**
     * Gets the length of a specific cell
     */
    internal fun getCellLength(cell: I?): Double {
        if (cell == null) return 0.0
        if (fixedCellSize != null) return fixedCellSize

        return if (true)
            cell.layoutBounds.height
        else
            cell.layoutBounds.width
    }

    /**
     * Gets the breadth of a specific cell
     */
    internal fun getCellBreadth(cell: Cell<*>?): Double {
        return if (true)
            cell!!.prefWidth(-1.0)
        else
            cell!!.prefHeight(-1.0)
    }

    /**
     * Gets the layout position of the cell along the length axis
     */
    internal fun getCellPosition(cell: I?): Double {
        if (cell == null) return 0.0

        return cell.layoutY
    }

    private fun positionCell(cell: I?, position: Double) {
        cell!!.layoutX = 0.0
        cell.setLayoutY(snapSizeY(position))
    }

    private fun resizeCellSize(cell: I?) {
        if (cell == null) return

        if (true) {
            val width = Math.max(maxPrefBreadth, viewportBreadth)
            cell.resize(width, if (fixedCellSize != null) fixedCellSize else
                Utils.boundedSize(cell.prefHeight(width), cell.minHeight(width), cell.maxHeight(width)))
        } else {
            val height = Math.max(maxPrefBreadth, viewportBreadth)
            cell.resize(if (fixedCellSize != null) fixedCellSize else
                Utils.boundedSize(cell.prefWidth(height), cell.minWidth(height), cell.maxWidth(height)), height)
        }
    }

    /**
     * Adds all the cells prior to and including the given currentIndex, until
     * no more can be added without falling off the flow. The startOffset
     * indicates the distance from the leading edge (top) of the viewport to
     * the leading edge (top) of the currentIndex.
     */
    internal fun addLeadingCells(currentIndex: Int, startOffset: Double) {
        // The offset will keep track of the distance from the top of the
        // viewport to the top of the current index. We will increment it
        // as we lay out leading cells.
        var offset = startOffset
        // The index is the absolute index of the cell being laid out
        var index = currentIndex

        // Offset should really be the bottom of the current index
        var first = true // first time in, we just fudge the offset and let
        // it be the top of the current index then redefine
        // it as the bottom of the current index thereafter
        // while we have not yet laid out so many cells that they would fall
        // off the flow, we will continue to create and add cells. The
        // offset is our indication of whether we can lay out additional
        // cells. If the offset is ever < 0, except in the case of the very
        // first cell, then we must quit.
        var cell: I?

        // special case for the position == 1.0, skip adding last invisible cell
        if (index == getCellCount() && offset == viewportLength) {
            index--
            first = false
        }
        while (index >= 0 && (offset > 0 || first)) {
            cell = getAvailableCell(index)
            setCellIndex(cell, index)
            resizeCellSize(cell) // resize must be after config
            cells.addFirst(cell)

            // A little gross but better than alternatives because it reduces
            // the number of times we have to update a cell or compute its
            // size. The first time into this loop "offset" is actually the
            // top of the current index. On all subsequent visits, it is the
            // bottom of the current index.
            if (first) {
                first = false
            } else {
                offset -= getCellLength(cell)
            }

            // Position the cell, and update the maxPrefBreadth variable as we go.
            positionCell(cell, offset)
            maxPrefBreadth = Math.max(maxPrefBreadth, getCellBreadth(cell))
            cell.isVisible = true
            --index
        }

        // There are times when after laying out the cells we discover that
        // the top of the first cell which represents index 0 is below the top
        // of the viewport. In these cases, we don't adjust the cells up
        // or reset the mapper position. This might happen when items got
        // removed at the top or when the viewport size increased.
        if (cells.size > 0) {
            cell = cells.first
            val firstIndex = getCellIndex(cell)
            val firstCellPos = getCellPosition(cell)
            if (firstIndex == 0 && firstCellPos > 0) {
            }
        } else {
            // reset scrollbar to top, so if the flow sees cells again it starts at the top
            vbar.value = 0.0
        }
    }

    /**
     * Adds all the trailing cells that come *after* the last index in
     * the cells ObservableList.
     */
    internal fun addTrailingCells(fillEmptyCells: Boolean): Boolean {
        // If cells is empty then addLeadingCells bailed for some reason and
        // we're hosed, so just punt
        if (cells.isEmpty()) return false

        // While we have not yet laid out so many cells that they would fall
        // off the flow, so we will continue to create and add cells. When the
        // offset becomes greater than the width/height of the flow, then we
        // know we cannot add any more cells.
        val startCell = cells.last
        var offset = getCellPosition(startCell) + getCellLength(startCell)
        var index = getCellIndex(startCell) + 1
        val cellCount = getCellCount()
        var filledWithNonEmpty = index <= cellCount

        val viewportLength = viewportLength

        // Fix for RT-37421, which was a regression caused by RT-36556
        if (offset < 0 && !fillEmptyCells) {
            return false
        }

        //
        // RT-36507: viewportLength gives the maximum number of
        // additional cells that should ever be able to fit in the viewport if
        // every cell had a height of 1. If index ever exceeds this count,
        // then offset is not incrementing fast enough, or at all, which means
        // there is something wrong with the cell size calculation.
        //
        val maxCellCount = viewportLength
        while (offset < viewportLength) {
            if (index >= cellCount) {
                if (offset < viewportLength) filledWithNonEmpty = false
                if (!fillEmptyCells) return filledWithNonEmpty
                // RT-36507 - return if we've exceeded the maximum
                if (index > maxCellCount) {
                        System.err.println("index exceeds maxCellCount. Check size calculations for " + startCell!!.javaClass)
                    return filledWithNonEmpty
                }
            }
            val cell = getAvailableCell(index)
            setCellIndex(cell, index)
            resizeCellSize(cell) // resize happens after config!
            cells.addLast(cell)

            // Position the cell and update the max pref
            positionCell(cell, offset)
            maxPrefBreadth = Math.max(maxPrefBreadth, getCellBreadth(cell))

            offset += getCellLength(cell)
            cell.isVisible = true
            ++index
        }

        // Discover whether the first cell coincides with index #0. If after
        // adding all the trailing cells we find that a) the first cell was
        // not index #0 and b) there are trailing cells, then we have a
        // problem. We need to shift all the cells down and add leading cells,
        // one at a time, until either the very last non-empty cells is aligned
        // with the bottom OR we have laid out cell index #0 at the first
        // position.
        var firstCell = cells.first
        index = getCellIndex(firstCell)
        val lastNonEmptyCell = lastVisibleCell
        var start = getCellPosition(firstCell)
        val end = getCellPosition(lastNonEmptyCell) + getCellLength(lastNonEmptyCell)
        if ((index != 0 || index == 0 && start < 0) && fillEmptyCells &&
                lastNonEmptyCell != null && getCellIndex(lastNonEmptyCell) == cellCount - 1 && end < viewportLength) {

            var prospectiveEnd = end
            val distance = viewportLength - end
            while (prospectiveEnd < viewportLength && index != 0 && -start < distance) {
                index--
                val cell = getAvailableCell(index)
                setCellIndex(cell, index)
                resizeCellSize(cell) // resize must be after config
                cells.addFirst(cell)
                val cellLength = getCellLength(cell)
                start -= cellLength
                prospectiveEnd += cellLength
                positionCell(cell, start)
                maxPrefBreadth = Math.max(maxPrefBreadth, getCellBreadth(cell))
                cell.isVisible = true
            }

            // The amount by which to translate the cells down
            firstCell = cells.first
            start = getCellPosition(firstCell)
            var delta = viewportLength - end
            if (getCellIndex(firstCell) == 0 && delta > -start) {
                delta = -start
            }
            // Move things
            for (i in cells.indices) {
                val cell = cells[i]
                positionCell(cell, getCellPosition(cell) + delta)
            }

            // Check whether the first cell, subsequent to our adjustments, is
            // now index #0 and aligned with the top. If so, change the position
            // to be at 0 instead of 1.
            start = getCellPosition(firstCell)
            if (getCellIndex(firstCell) == 0 && start == 0.0) {
                setPosition(0.0)
            } else if (getPosition() != 1.0) {
                setPosition(1.0)
            }
        }

        return filledWithNonEmpty
    }

    internal fun reconfigureCells() {
        needsReconfigureCells = true
        requestLayout()
    }

    internal fun recreateCells() {
        needsRecreateCells = true
        requestLayout()
    }

    internal fun rebuildCells() {
        needsRebuildCells = true
        requestLayout()
    }

    internal fun setCellDirty(index: Int) {
        dirtyCells.set(index)
        requestLayout()
    }

    private fun updateViewportDimensions() {
        viewportBreadth = width - snapSizeX(vbar.prefWidth(-1.0))
        viewportLength = height
    }

    private fun initViewport() {
        // Initialize the viewportLength and viewportBreadth to match the
        // width/height of the flow

        updateViewportDimensions()
    }

    private fun updateScrollBarsAndCells(recreate: Boolean) {
        val lengthBar = vbar

        // We may have adjusted the viewport length and breadth after the
        // layout due to scroll bars becoming visible. So we need to perform
        // a follow up pass and resize and shift all the cells to fit the
        // viewport. Note that the prospective viewport size is always >= the
        // final viewport size, so we don't have to worry about adding
        // cells during this cleanup phase.
        fitCells()

        // Update cell positions.
        // When rebuilding the cells, we add the cells and along the way compute
        // the maxPrefBreadth. Based on the computed value, we may add
        // the breadth scrollbar which changes viewport length, so we need
        // to re-position the cells.
        if (!cells.isEmpty()) {
            val currOffset = -computeViewportOffset(getPosition())
            val currIndex = computeCurrentIndex() - cells.first!!.index
            val indices = cells.indices

            // position leading cells
            var offset = currOffset

            run {
                var i = currIndex - 1
                while (i in indices) {
                    val cell = cells[i]

                    offset -= getCellLength(cell)

                    positionCell(cell, offset)
                    i--
                }
            }

            // position trailing cells
            offset = currOffset
            var i = currIndex
            while (i in indices) {
                val cell = cells[i]
                positionCell(cell, offset)

                offset += getCellLength(cell)
                i++
            }
        }

        var sumCellLength = 0.0
        val flowLength = (height)

        // determine how many cells there are on screen so that the scrollbar
        // thumb can be appropriately sized
        if (recreate && (lengthBar.isVisible || Utils.IS_TOUCH_SUPPORTED)) {
            val cellCount = getCellCount()
            var numCellsVisibleOnScreen = 0
            var i = 0
            val max = cells.size
            while (i < max) {
                val cell = cells[i]
                if (cell != null && !cell.isEmpty) {
                    sumCellLength += cell.height
                    if (sumCellLength > flowLength) {
                        break
                    }

                    numCellsVisibleOnScreen++
                }
                i++
            }

            lengthBar.max = 1.0
            if (numCellsVisibleOnScreen == 0 && cellCount == 1) {
                // special case to help resolve RT-17701 and the case where we have
                // only a single row and it is bigger than the viewport
                lengthBar.visibleAmount = flowLength / sumCellLength
            } else {
                lengthBar.visibleAmount = (numCellsVisibleOnScreen / cellCount.toFloat()).toDouble()
            }
        }

        vbar.resizeRelocate(viewportBreadth, 0.0, vbar.prefWidth(viewportLength), viewportLength)

        clipView.resize(snapSizeX(viewportBreadth),
                snapSizeY(viewportLength))

        // If the viewportLength becomes large enough that all cells fit
        // within the viewport, then we want to update the value to match.
        if (getPosition() != lengthBar.value) {
            lengthBar.value = getPosition()
        }
    }

    /**
     * Adjusts the cells location and size if necessary. The breadths of all
     * cells will be adjusted to fit the viewportWidth or maxPrefBreadth, and
     * the layout position will be updated if necessary based on index and
     * offset.
     */
    private fun fitCells() {
        val size = Math.max(maxPrefBreadth, viewportBreadth)

        // Note: Do not optimise this loop by pre-calculating the cells size and
        // storing that into a int value - this can lead to RT-32828
        for (i in cells.indices) {
            val cell = cells[i]
            cell!!.resize(size, cell.prefHeight(size))
        }
    }

    private fun cull() {
        val viewportLength = viewportLength
        for (i in cells.indices.reversed()) {
            val cell = cells[i]
            val cellSize = getCellLength(cell)
            val cellStart = getCellPosition(cell)
            val cellEnd = cellStart + cellSize
            if (cellStart >= viewportLength || cellEnd < 0) {
                addToPile(cells.removeAt(i))
            }
        }
    }

    /**
     * After using the accum cell, it needs to be released!
     */
    private fun releaseCell(cell: I?) {
        if (accumCell != null && cell === accumCell) {
            accumCell!!.updateIndex(-1)
        }
    }

    /**
     * This method is an experts-only method - if the requested index is not
     * already an existing visible cell, it will create a cell for the
     * given index and insert it into the sheet. From that point on it will be
     * unmanaged, and is up to the caller of this method to manage it.
     */
    internal fun getPrivateCell(index: Int): I? {
        var cell: I? = null

        // If there are cells, then we will attempt to get an existing cell
        if (!cells.isEmpty()) {
            // First check the cells that have already been created and are
            // in use. If this call returns a value, then we can use it
            cell = getVisibleCell(index)
            if (cell != null) {
                // Force the underlying text inside the cell to be updated
                // so that when the screen reader runs, it will match the
                // text in the cell (force updateDisplayedText())
                cell.layout()
                return cell
            }
        }

        // check the existing sheet children
        if (cell == null) {
            for (i in sheetChildren.indices) {
                @Suppress("UNCHECKED_CAST")
                val _cell = sheetChildren[i] as I
                if (getCellIndex(_cell) == index) {
                    return _cell
                }
            }
        }

        val cellFactory = getCellFactory()
        if (cellFactory != null) {
            cell = cellFactory.call(this)
        }

        if (cell != null) {
            setCellIndex(cell, index)
            resizeCellSize(cell)
            cell.isVisible = false
            sheetChildren.add(cell)
            privateCells.add(cell)
        }

        return cell
    }

    private fun releaseAllPrivateCells() {
        sheetChildren.removeAll(privateCells)
    }

    /**
     * Puts the given cell onto the pile. This is called whenever a cell has
     * fallen off the flow's start.
     */
    private fun addToPile(cell: I) {
        pile.addLast(cell)
    }

    private fun cleanPile() {
        var wasFocusOwner = false

        var i = 0
        val max = pile.size
        while (i < max) {
            val cell = pile[i]
            wasFocusOwner = wasFocusOwner || doesCellContainFocus(cell!!)
            cell!!.isVisible = false
            i++
        }

        // Fix for RT-35876: Rather than have the cells do weird things with
        // focus (in particular, have focus jump between cells), we return focus
        // to the VirtualFlow itself.
        if (wasFocusOwner) {
            requestFocus()
        }
    }

    private fun doesCellContainFocus(c: Cell<*>): Boolean {
        val scene = c.scene
        val focusOwner = scene?.focusOwner

        if (focusOwner != null) {
            if (c == focusOwner) {
                return true
            }

            var p: Parent? = focusOwner.parent
            while (p != null && p !is KVirtualFlow<*, *>) {
                if (c == p) {
                    return true
                }
                p = p.parent
            }
        }

        return false
    }

    private fun getPrefBreadth(oppDimension: Double): Double {
        var max = getMaxCellWidth(10)

        // This primarily exists for the case where we do not want the breadth
        // to grow to ensure a golden ratio between width and height (for example,
        // when a ListView is used in a ComboBox - the width should not grow
        // just because items are being added to the ListView)
        if (oppDimension > -1) {
            val prefLength = prefLength
            max = Math.max(max, prefLength * GOLDEN_RATIO_MULTIPLIER)
        }

        return max
    }

    internal fun getMaxCellWidth(rowsToCount: Int): Double {
        var max = 0.0

        // we always measure at least one row
        val rows = Math.max(1, if (rowsToCount == -1) getCellCount() else rowsToCount)
        for (i in 0 until rows) {
            max = Math.max(max, getCellBreadth(i))
        }
        return max
    }

    // Old PositionMapper
    /**
     * Given a position value between 0 and 1, compute and return the viewport
     * offset from the "current" cell associated with that position value.
     * That is, if the return value of this function where used as a translation
     * factor for a sheet that contained all the items, then the current
     * item would end up positioned correctly.
     */
    private fun computeViewportOffset(position: Double): Double {
        val p = Utils.clamp(0.0, position, 1.0)
        val fractionalPosition = p * getCellCount()
        val cellIndex = fractionalPosition.toInt()
        val fraction = fractionalPosition - cellIndex
        val cellSize = getCellLength(cellIndex)
        val pixelOffset = cellSize * fraction
        val viewportOffset = viewportLength * p
        return pixelOffset - viewportOffset
    }

    private fun adjustPositionToIndex(index: Int) {
        val cellCount = getCellCount()
        if (cellCount <= 0) {
            setPosition(0.0)
        } else {
            setPosition(index.toDouble() / cellCount)
        }
    }

    /**
     * Adjust the position based on a delta of pixels. If negative, then the
     * position will be adjusted negatively. If positive, then the position will
     * be adjusted positively. If the pixel amount is too great for the range of
     * the position, then it will be clamped such that position is always
     * strictly between 0 and 1
     */
    private fun adjustByPixelAmount(numPixels: Double) {
        if (numPixels == 0.0) return
        // Starting from the current cell, we move in the direction indicated
        // by numPixels one cell at a team. For each cell, we discover how many
        // pixels the "position" line would move within that cell, and adjust
        // our count of numPixels accordingly. When we come to the "final" cell,
        // then we can take the remaining number of pixels and multiply it by
        // the "travel rate" of "p" within that cell to get the delta. Add
        // the delta to "p" to get position.

        // get some basic info about the list and the current cell
        val forward = numPixels > 0
        val cellCount = getCellCount()
        val fractionalPosition = getPosition() * cellCount
        var cellIndex = fractionalPosition.toInt()
        if (forward && cellIndex == cellCount) return
        var cellSize = getCellLength(cellIndex)
        val fraction = fractionalPosition - cellIndex
        val pixelOffset = cellSize * fraction

        // compute the percentage of "position" that represents each cell
        val cellPercent = 1.0 / cellCount

        // To help simplify the algorithm, we pretend as though the current
        // position is at the beginning of the current cell. This reduces some
        // of the corner cases and provides a simpler algorithm without adding
        // any overhead to performance.
        var start = computeOffsetForCell(cellIndex)
        var end = cellSize + computeOffsetForCell(cellIndex + 1)

        // We need to discover the distance that the fictional "position line"
        // would travel within this cell, from its current position to the end.
        var remaining = end - start

        // Keep track of the number of pixels left to travel
        var n = if (forward)
            numPixels + pixelOffset - viewportLength * getPosition() - start
        else
            -numPixels + end - (pixelOffset - viewportLength * getPosition())

        // "p" represents the most recent value for position. This is always
        // based on the edge between two cells, except at the very end of the
        // algorithm where it is added to the computed "p" offset for the final
        // value of Position.
        var p = cellPercent * cellIndex

        // Loop over the cells one at a time until either we reach the end of
        // the cells, or we find that the "n" will fall within the cell we're on
        while (n > remaining && (forward && cellIndex < cellCount - 1 || !forward && cellIndex > 0)) {
            if (forward) cellIndex++ else cellIndex--
            n -= remaining
            cellSize = getCellLength(cellIndex)
            start = computeOffsetForCell(cellIndex)
            end = cellSize + computeOffsetForCell(cellIndex + 1)
            remaining = end - start
            p = cellPercent * cellIndex
        }

        // if remaining is < n, then we must have hit an end, so as a
        // fast path, we can just set position to 1.0 or 0.0 and return
        // because we know we hit the end
        if (n > remaining) {
            setPosition((if (forward) 1.0f else 0.0f).toDouble())
        } else if (forward) {
            val rate = cellPercent / Math.abs(end - start)
            setPosition(p + rate * n)
        } else {
            val rate = cellPercent / Math.abs(end - start)
            setPosition(p + cellPercent - rate * n)
        }
    }

    private fun computeCurrentIndex(): Int {
        return (getPosition() * getCellCount()).toInt()
    }

    /**
     * Given an item index, this function will compute and return the viewport
     * offset from the beginning of the specified item. Notice that because each
     * item has the same percentage of the position dedicated to it, and since
     * we are measuring from the start of each item, this is a very simple
     * calculation.
     */
    private fun computeOffsetForCell(itemIndex: Int): Double {
        val cellCount = getCellCount().toDouble()
        val p = Utils.clamp(0.0, itemIndex.toDouble(), cellCount) / cellCount
        return -(viewportLength * p)
    }

    //    /**
    //     * Adjust the position based on a chunk of pixels. The position is based
    //     * on the start of the scrollbar position.
    //     */
    //    private void adjustByPixelChunk(double numPixels) {
    //        setPosition(0);
    //        adjustByPixelAmount(numPixels);
    //    }
    // end of old PositionMapper code


    /***************************************************************************
     * *
     * Support classes                                                         *
     * *
     */

    /**
     * A simple extension to Region that ensures that anything wanting to flow
     * outside of the bounds of the Region is clipped.
     */
    internal class ClippedContainer(flow: KVirtualFlow<*, *>?) : Region() {

        /**
         * The Node which is embedded within this `ClipView`.
         */
        var node: Node? = null
            set(n) {
                field = n

                children.clear()
                children.add(this.node)
            }

        private val clipRect: Rectangle

        fun setClipY(clipY: Double) {
            layoutY = -clipY
            clipRect.layoutY = clipY
        }

        init {
            if (flow == null) {
                throw IllegalArgumentException("VirtualFlow can not be null")
            }

            styleClass.add("clipped-container")

            // clipping
            clipRect = Rectangle()
            clipRect.isSmooth = false
            clip = clipRect
            // --- clipping

            super.widthProperty().addListener { _ -> clipRect.width = width }
            super.heightProperty().addListener { _ -> clipRect.height = height }
        }
    }

    /**
     * A List-like implementation that is exceedingly efficient for the purposes
     * of the VirtualFlow. Typically there is not much variance in the number of
     * cells -- it is always some reasonably consistent number. Yet for efficiency
     * in code, we like to use a linked list implementation so as to append to
     * start or append to end. However, at times when we need to iterate, LinkedList
     * is expensive computationally as well as requiring the construction of
     * temporary iterators.
     *
     *
     * This linked list like implementation is done using an array. It begins by
     * putting the first item in the center of the allocated array, and then grows
     * outward (either towards the first or last of the array depending on whether
     * we are inserting at the head or tail). It maintains an index to the start
     * and end of the array, so that it can efficiently expose iteration.
     *
     *
     * This class is package private solely for the sake of testing.
     */
    internal class ArrayLinkedList<T> : AbstractList<T>() {
        /**
         * The array list backing this class. We default the size of the array
         * list to be fairly large so as not to require resizing during normal
         * use, and since that many ArrayLinkedLists won't be created it isn't
         * very painful to do so.
         */
        private val array: ArrayList<T?>

        private var firstIndex = -1
        private var lastIndex = -1

        val first: T?
            get() = if (firstIndex == -1) null else array[firstIndex]

        val last: T?
            get() = if (lastIndex == -1) null else array[lastIndex]

        init {
            array = ArrayList(50)

            for (i in 0..49) {
                array.add(null)
            }
        }

        fun addFirst(cell: T?) {
            // if firstIndex == -1 then that means this is the first item in the
            // list and we need to initialize firstIndex and lastIndex
            if (firstIndex == -1) {
                lastIndex = array.size / 2
                firstIndex = lastIndex
                array.set(firstIndex, cell)
            } else if (firstIndex == 0) {
                // we're already at the head of the array, so insert at position
                // 0 and then increment the lastIndex to compensate
                array.add(0, cell)
                lastIndex++
            } else {
                // we're not yet at the head of the array, so insert at the
                // firstIndex - 1 position and decrement first position
                array.set(--firstIndex, cell)
            }
        }

        fun addLast(cell: T) {
            // if lastIndex == -1 then that means this is the first item in the
            // list and we need to initialize the firstIndex and lastIndex
            if (firstIndex == -1) {
                lastIndex = array.size / 2
                firstIndex = lastIndex
                array[lastIndex] = cell
            } else if (lastIndex == array.size - 1) {
                // we're at the end of the array so need to "add" so as to force
                // the array to be expanded in size
                array.add(++lastIndex, cell)
            } else {
                array[++lastIndex] = cell
            }
        }

        override val size: Int
            get() = if (firstIndex == -1) 0 else lastIndex - firstIndex + 1

        override fun isEmpty(): Boolean {
            return firstIndex == -1
        }

        override fun get(index: Int): T? {
            return if (index > lastIndex - firstIndex || index < 0) {
                // Commented out exception due to RT-29111
                // throw new java.lang.ArrayIndexOutOfBoundsException();
                null
            } else array[firstIndex + index]

        }

        override fun clear() {
            for (i in array.indices) {
                array.set(i, null)
            }

            lastIndex = -1
            firstIndex = lastIndex
        }

        fun removeFirst(): T? {
            return if (isEmpty()) null else removeAt(0)
        }

        fun removeLast(): T? {
            return if (isEmpty()) null else removeAt(lastIndex - firstIndex)
        }

        override fun removeAt(index: Int): T {
            if (index > lastIndex - firstIndex || index < 0) {
                throw ArrayIndexOutOfBoundsException()
            }

            // if the index == 0, then we're removing the first
            // item and can simply set it to null in the array and increment
            // the firstIndex unless there is only one item, in which case
            // we have to also set first & last index to -1.
            if (index == 0) {
                val cell = array[firstIndex]
                array.set(firstIndex, null)
                if (firstIndex == lastIndex) {
                    lastIndex = -1
                    firstIndex = lastIndex
                } else {
                    firstIndex++
                }
                return cell!!
            } else if (index == lastIndex - firstIndex) {
                // if the index == lastIndex - firstIndex, then we're removing the
                // last item and can simply set it to null in the array and
                // decrement the lastIndex
                val cell = array[lastIndex]
                array.set(lastIndex--, null)
                return cell!!
            } else {
                // if the index is somewhere in between, then we have to remove the
                // item and decrement the lastIndex
                val cell = array[firstIndex + index]
                array.set(firstIndex + index, null)
                for (i in firstIndex + index + 1..lastIndex) {
                    array[i - 1] = array[i]
                }
                array.set(lastIndex--, null)
                return cell!!
            }
        }
    }

    companion object {

        /***************************************************************************
         * *
         * Static fields                                                           *
         * *
         */

        /**
         * Scroll events may request to scroll about a number of "lines". We first
         * decide how big one "line" is - for fixed cell size it's clear,
         * for variable cell size we settle on a single number so that the scrolling
         * speed is consistent. Now if the line is so big that
         * MIN_SCROLLING_LINES_PER_PAGE of them don't fit into one page, we make
         * them smaller to prevent the scrolling step to be too big (perhaps
         * even more than one page).
         */
        private val MIN_SCROLLING_LINES_PER_PAGE = 8

        /**
         * Indicates that this is a newly created cell and we need call processCSS for it.
         *
         * See RT-23616 for more details.
         */
        private val NEW_CELL = "newcell"

        private val GOLDEN_RATIO_MULTIPLIER = 0.618033987
    }
}
