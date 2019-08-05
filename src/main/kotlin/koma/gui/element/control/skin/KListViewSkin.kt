package koma.gui.element.control.skin

import javafx.beans.Observable
import javafx.beans.WeakInvalidationListener
import javafx.collections.*
import javafx.event.EventHandler
import javafx.geometry.Orientation
import javafx.scene.AccessibleAction
import javafx.scene.AccessibleAttribute
import javafx.scene.Node
import javafx.scene.control.Label
import javafx.scene.control.ListCell
import javafx.scene.control.ListView
import javafx.scene.input.MouseEvent
import javafx.scene.layout.StackPane
import javafx.util.Callback
import koma.gui.element.control.behavior.ListViewBehavior
import java.util.*


class KListViewSkin<T>(
        control: ListView<T>
): KVirtualContainerBase<ListView<T>, ListCell<T>, T>(control) {

    /**
     * Region placed over the top of the flow (and possibly the header row) if
     * there is no data.
     */
    private var placeholderRegion: StackPane? = null
    private var placeholderNode: Node? = null

    private var listViewItems: ObservableList<T>? = null
    private val itemsChangeListener = { _: Observable -> updateListViewItems() }

    private var needCellsRebuilt = true
    private var needCellsReconfigured = false

    override var itemCount = -1
    private val behavior: ListViewBehavior<T>?


    /***************************************************************************
     * *
     * Listeners                                                               *
     * *
     */

    private val propertiesMapListener = MapChangeListener<Any,Any> { c: MapChangeListener.Change<out Any, out Any> ->
        val RECREATE = "recreateKey"
        if (!c.wasAdded()) {
        } else if (RECREATE.equals(c.getKey())) {
            needCellsRebuilt = true
            skinnable.requestLayout()
            skinnable.properties.remove(RECREATE)
        }
    }

    private val listViewItemsListener = ListChangeListener<T> { c ->
        while (c.next()) {
            if (c.wasReplaced()) {
                // RT-28397: Support for when an item is replaced with itself (but
                // updated internal values that should be shown visually).
                // This code was updated for RT-36714 to not update all cells,
                // just those affected by the change
                for (i in c.from until c.to) {
                    flow.setCellDirty(i)
                }

                break
            } else if (c.removedSize == itemCount) {
                // RT-22463: If the user clears out an items list then we
                // should reset all cells (in particular their contained
                // items) such that a subsequent addition to the list of
                // an item which equals the old item (but is rendered
                // differently) still displays as expected (i.e. with the
                // updated display, not the old display).
                itemCount = 0
                break
            }
        }

        // fix for RT-37853
        skinnable.edit(-1)

        markItemCountDirty()
        skinnable.requestLayout()
    }

    private val weakListViewItemsListener = WeakListChangeListener(listViewItemsListener)

    private val RECREATE = "recreateKey"

    init {

        // install default input map for the ListView control
        behavior = ListViewBehavior(control)
        //        control.setInputMap(behavior.getInputMap());

        // init the behavior 'closures'
        behavior.setOnFocusPreviousRow(Runnable { onFocusPreviousCell() })
        behavior.setOnFocusNextRow (Runnable{ onFocusNextCell() })
        behavior.setOnMoveToFirstCell (Runnable{ onMoveToFirstCell() })
        behavior.setOnMoveToLastCell (Runnable{ onMoveToLastCell() })
        behavior.setOnSelectPreviousRow (Runnable{ onSelectPreviousCell() })
        behavior.setOnSelectNextRow (Runnable{ onSelectNextCell() })
        behavior.setOnScrollPageDown(Callback<Boolean, Int> { this.onScrollPageDown(it) })
        behavior.setOnScrollPageUp(Callback<Boolean, Int> { this.onScrollPageUp(it) })

        updateListViewItems()

        // init the VirtualFlow
        flow.id = "virtual-flow"
        flow.isPannable = IS_PANNABLE
        flow.isVertical = control.orientation == Orientation.VERTICAL
        flow.setCellFactory(Callback { createCell() })
        flow.setFixedCellSize(control.fixedCellSize)
        children.add(flow)

        val ml: EventHandler<MouseEvent> = EventHandler {
            // RT-15127: cancel editing on scroll. This is a bit extreme
            // (we are cancelling editing on touching the scrollbars).
            // This can be improved at a later date.
            if (control.editingIndex > -1) {
                control.edit(-1)
            }

            // This ensures that the list maintains the focus, even when the vbar
            // and hbar controls inside the flow are clicked. Without this, the
            // focus border will not be shown when the user interacts with the
            // scrollbars, and more importantly, keyboard navigation won't be
            // available to the user.
            if (control.isFocusTraversable) {
                control.requestFocus()
            }
        }
        flow.vbar.addEventFilter(MouseEvent.MOUSE_PRESSED, ml)
        flow.hbar.addEventFilter(MouseEvent.MOUSE_PRESSED, ml)

        updateItemCount()

        control.itemsProperty().addListener(WeakInvalidationListener(itemsChangeListener))

        val properties = control.properties
        properties.remove(RECREATE)
        properties.addListener(propertiesMapListener)

        // Register listeners
        registerChangeListener(control.itemsProperty()) { updateListViewItems() }
        registerChangeListener(control.orientationProperty()) {
            flow.isVertical = control.orientation == Orientation.VERTICAL }
        registerChangeListener(control.cellFactoryProperty()) { _ -> flow.recreateCells() }
        registerChangeListener(control.parentProperty()) { _ ->
            if (control.parent != null && control.isVisible) {
                control.requestLayout()
            }
        }
        registerChangeListener(control.placeholderProperty()) { _ -> updatePlaceholderRegionVisibility() }
        registerChangeListener(control.fixedCellSizeProperty()
        ) { _ -> flow.setFixedCellSize(control.fixedCellSize) }
    }


    /***************************************************************************
     * *
     * Public API                                                              *
     * *
     */

    /** {@inheritDoc}  */
    override fun dispose() {
        super.dispose()

        behavior?.dispose()
    }

    /** {@inheritDoc}  */
    override fun layoutChildren(x: Double, y: Double,
                                w: Double, h: Double) {
        super.layoutChildren(x, y, w, h)

        if (needCellsRebuilt) {
            flow.rebuildCells()
        } else if (needCellsReconfigured) {
            flow.reconfigureCells()
        }

        needCellsRebuilt = false
        needCellsReconfigured = false

        if (itemCount == 0) {
            // show message overlay instead of empty listview
            if (placeholderRegion != null) {
                placeholderRegion!!.isVisible = w > 0 && h > 0
                placeholderRegion!!.resizeRelocate(x, y, w, h)
            }
        } else {
            flow.resizeRelocate(x, y, w, h)
        }
    }

    /** {@inheritDoc}  */
    override fun computePrefWidth(height: Double, topInset: Double, rightInset: Double, bottomInset: Double, leftInset: Double): Double {
        checkState()

        if (itemCount == 0) {
            if (placeholderRegion == null) {
                updatePlaceholderRegionVisibility()
            }
            if (placeholderRegion != null) {
                return placeholderRegion!!.prefWidth(height) + leftInset + rightInset
            }
        }

        return computePrefHeight(-1.0, topInset, rightInset, bottomInset, leftInset) * 0.618033987
    }

    /** {@inheritDoc}  */
    override fun computePrefHeight(width: Double, topInset: Double, rightInset: Double, bottomInset: Double, leftInset: Double): Double {
        return 400.0
    }

    /** {@inheritDoc}  */
    override fun updateItemCount() {

        val oldCount = itemCount
        val newCount = if (listViewItems == null) 0 else listViewItems!!.size

        itemCount = newCount

        flow.setCellCount(newCount)

        updatePlaceholderRegionVisibility()
        if (newCount != oldCount) {
            requestRebuildCells()
        } else {
            needCellsReconfigured = true
        }
    }

    /** {@inheritDoc}  */
    override fun queryAccessibleAttribute(attribute: AccessibleAttribute?, vararg parameters: Any): Any? {
        when (attribute) {
            AccessibleAttribute.FOCUS_ITEM -> {
                val fm = skinnable.focusModel
                var focusedIndex = fm.focusedIndex
                if (focusedIndex == -1) {
                    if (placeholderRegion != null && placeholderRegion!!.isVisible) {
                        return placeholderRegion!!.children[0]
                    }
                    if (itemCount > 0) {
                        focusedIndex = 0
                    } else {
                        return null
                    }
                }
                return flow.getPrivateCell(focusedIndex)
            }
            AccessibleAttribute.ITEM_COUNT -> return itemCount
            AccessibleAttribute.ITEM_AT_INDEX -> {
                val rowIndex = parameters[0] as Int
                return if (0 <= rowIndex && rowIndex < itemCount) {
                    flow.getPrivateCell(rowIndex)
                } else null
            }
            AccessibleAttribute.SELECTED_ITEMS -> {
                val sm = skinnable.selectionModel
                val indices = sm.selectedIndices
                val selection = ArrayList<ListCell<T>>(indices.size)
                for (i in indices) {
                    val row = flow.getPrivateCell(i)
                    if (row != null) selection.add(row)
                }
                return FXCollections.observableArrayList<List<Node>>(selection)
            }
            AccessibleAttribute.VERTICAL_SCROLLBAR -> return flow.vbar
            AccessibleAttribute.HORIZONTAL_SCROLLBAR -> return flow.hbar
            else -> return super.queryAccessibleAttribute(attribute, *parameters)
        }
    }

    /** {@inheritDoc}  */
    override fun executeAccessibleAction(action: AccessibleAction, vararg parameters: Any) {
        when (action) {
           AccessibleAction.SHOW_ITEM -> {
                val item = parameters[0] as Node
                if (item is ListCell<*>) {
                    val cell = item as ListCell<T>
                    flow.scrollTo(cell.index)
                }
            }
          AccessibleAction.SET_SELECTED_ITEMS -> {
                val items = parameters[0] as ObservableList<Node>
              val sm = skinnable.selectionModel
              if (sm != null) {
                  sm.clearSelection()
                  for (item in items) {
                      if (item is ListCell<*>) {
                          val cell = item as ListCell<T>
                          sm.select(cell.index)
                      }
                  }
              }
          }
            else -> super.executeAccessibleAction(action, parameters)
        }
    }


    /***************************************************************************
     * *
     * Private implementation                                                  *
     * *
     */

    /** {@inheritDoc}  */
    private fun createCell(): ListCell<T> {
        val cell: ListCell<T>
        if (skinnable.cellFactory != null) {
            cell = skinnable.cellFactory.call(skinnable)
        } else {
            cell = createDefaultCellImpl()
        }

        cell.updateListView(skinnable)

        return cell
    }

    private fun updateListViewItems() {
        if (listViewItems != null) {
            listViewItems!!.removeListener(weakListViewItemsListener)
        }

        this.listViewItems = skinnable.items

        if (listViewItems != null) {
            listViewItems!!.addListener(weakListViewItemsListener)
        }

        markItemCountDirty()
        skinnable.requestLayout()
    }

    private fun updatePlaceholderRegionVisibility() {
        val visible = itemCount == 0

        if (visible) {
            placeholderNode = skinnable.placeholder
            if (placeholderNode == null && !EMPTY_LIST_TEXT.isEmpty()) {
                placeholderNode = Label()
                (placeholderNode as Label).setText(EMPTY_LIST_TEXT)
            }

            if (placeholderNode != null) {
                if (placeholderRegion == null) {
                    placeholderRegion = StackPane()
                    placeholderRegion!!.styleClass.setAll("placeholder")
                    children.add(placeholderRegion)
                }

                placeholderRegion!!.children.setAll(placeholderNode)
            }
        }

        flow.isVisible = !visible
        if (placeholderRegion != null) {
            placeholderRegion!!.isVisible = visible
        }
    }

    private fun onFocusPreviousCell() {
        val fm = skinnable.focusModel ?: return
        flow.scrollTo(fm.focusedIndex)
    }

    private fun onFocusNextCell() {
        val fm = skinnable.focusModel ?: return
        flow.scrollTo(fm.focusedIndex)
    }

    private fun onSelectPreviousCell() {
        val sm = skinnable.selectionModel ?: return

        val pos = sm.selectedIndex
        if (pos < 0) return
        flow.scrollTo(pos)

        // Fix for RT-11299
        val cell = flow.firstVisibleCell
        if (cell == null || pos < cell.index) {
            val p = pos / itemCount.toDouble()
            flow.setPosition(p)
        }
    }

    private fun onSelectNextCell() {
        val sm = skinnable.selectionModel ?: return

        val pos = sm.selectedIndex
        flow.scrollTo(pos)

        // Fix for RT-11299
        val cell = flow.lastVisibleCell
        if (cell == null || cell.index < pos) {
            flow.setPosition ( pos / itemCount.toDouble())
        }
    }

    private fun onMoveToFirstCell() {
        flow.scrollTo(0)
        flow.setPosition(0.0)
    }

    private fun onMoveToLastCell() {
        //        SelectionModel sm = getSkinnable().getSelectionModel();
        //        if (sm == null) return;
        //
        val endPos = itemCount - 1
        //        sm.select(endPos);
        flow.scrollTo(endPos)
        flow.setPosition(1.0)
    }

    /**
     * Function used to scroll the container down by one 'page', although
     * if this is a horizontal container, then the scrolling will be to the right.
     */
    private fun onScrollPageDown(isFocusDriven: Boolean): Int {
        var lastVisibleCell: ListCell<T>? = flow.lastVisibleCellWithinViewPort ?: return -1

        val sm = skinnable.selectionModel
        val fm = skinnable.focusModel
        if (sm == null || fm == null) return -1

        val lastVisibleCellIndex = lastVisibleCell!!.index

        //        boolean isSelected = sm.isSelected(lastVisibleCellIndex) || fm.isFocused(lastVisibleCellIndex) || lastVisibleCellIndex == anchor;
        // isSelected represents focus OR selection
        val isSelected: Boolean
        if (isFocusDriven) {
            isSelected = lastVisibleCell.isFocused || fm.isFocused(lastVisibleCellIndex)
        } else {
            isSelected = lastVisibleCell.isSelected || sm.isSelected(lastVisibleCellIndex)
        }

        if (isSelected) {
            val isLeadIndex = isFocusDriven && fm.focusedIndex == lastVisibleCellIndex || !isFocusDriven && sm.selectedIndex == lastVisibleCellIndex

            if (isLeadIndex) {
                // if the last visible cell is selected, we want to shift that cell up
                // to be the top-most cell, or at least as far to the top as we can go.
                flow.scrollToTop(lastVisibleCell)

                val newLastVisibleCell = flow.lastVisibleCellWithinViewPort
                lastVisibleCell = newLastVisibleCell ?: lastVisibleCell
            }
        } else {
            // if the selection is not on the 'bottom' most cell, we firstly move
            // the selection down to that, without scrolling the contents, so
            // this is a no-op
        }

        val newSelectionIndex = lastVisibleCell.index
        flow.scrollTo(lastVisibleCell)
        return newSelectionIndex
    }

    /**
     * Function used to scroll the container up by one 'page', although
     * if this is a horizontal container, then the scrolling will be to the left.
     */
    private fun onScrollPageUp(isFocusDriven: Boolean): Int {
        var firstVisibleCell: ListCell<T>? = flow.firstVisibleCellWithinViewPort ?: return -1

        val sm = skinnable.selectionModel
        val fm = skinnable.focusModel
        if (sm == null || fm == null) return -1

        val firstVisibleCellIndex = firstVisibleCell!!.index

        // isSelected represents focus OR selection
        var isSelected: Boolean
        if (isFocusDriven) {
            isSelected = firstVisibleCell.isFocused || fm.isFocused(firstVisibleCellIndex)
        } else {
            isSelected = firstVisibleCell.isSelected || sm.isSelected(firstVisibleCellIndex)
        }

        if (isSelected) {
            val isLeadIndex = isFocusDriven && fm.focusedIndex == firstVisibleCellIndex || !isFocusDriven && sm.selectedIndex == firstVisibleCellIndex

            if (isLeadIndex) {
                // if the first visible cell is selected, we want to shift that cell down
                // to be the bottom-most cell, or at least as far to the bottom as we can go.
                flow.scrollToBottom(firstVisibleCell)

                val newFirstVisibleCell = flow.firstVisibleCellWithinViewPort
                firstVisibleCell = newFirstVisibleCell ?: firstVisibleCell
            }
        } else {
            // if the selection is not on the 'top' most cell, we firstly move
            // the selection up to that, without scrolling the contents, so
            // this is a no-op
        }

        val newSelectionIndex = firstVisibleCell.index
        flow.scrollTo(firstVisibleCell)
        return newSelectionIndex
    }

    companion object {

        /***************************************************************************
         * *
         * Static Fields                                                           *
         * *
         */

        // RT-34744 : IS_PANNABLE will be false unless
        // javafx.scene.control.skin.ListViewSkin.pannable
        // is set to true. This is done in order to make ListView functional
        // on embedded systems with touch screens which do not generate scroll
        // events for touch drag gestures.
        private val IS_PANNABLE = false


        /***************************************************************************
         * *
         * Internal Fields                                                         *
         * *
         */
        private const val EMPTY_LIST_TEXT =  "ListView.noContent"

        private fun <T> createDefaultCellImpl(): ListCell<T> {
            return object : ListCell<T>() {
                public override fun updateItem(item: T?, empty: Boolean) {
                    super.updateItem(item, empty)

                    if (empty) {
                        text = null
                        setGraphic(null)
                    } else if (item is Node) {
                        text = null
                        val currentNode = graphic
                        val newNode = item as Node?
                        if (currentNode == null || !currentNode.equals(newNode)) {
                            graphic = newNode
                        }
                    } else {
                        /**
                         * This label is used if the item associated with this cell is to be
                         * represented as a String. While we will lazily instantiate it
                         * we never clear it, being more afraid of object churn than a minor
                         * "leak" (which will not become a "major" leak).
                         */
                        text = item?.toString() ?: "null"
                        setGraphic(null)
                    }
                }
            }
        }
    }
}
