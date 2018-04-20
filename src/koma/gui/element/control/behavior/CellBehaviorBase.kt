package koma.gui.element.control.behavior

import javafx.event.EventHandler
import javafx.scene.control.*
import javafx.scene.input.MouseButton
import javafx.scene.input.MouseEvent
import koma.gui.element.control.inputmap.InputMap
import java.util.*

/**
 * Behaviors for standard cells types. Simply defines methods that subclasses
 * implement so that CellSkinBase has API to call.
 */
abstract class CellBehaviorBase<T : Cell<*>>
/***************************************************************************
 * *
 * Constructors                                                            *
 * *
 */
(control: T) : BehaviorBase<T>(control) {


    /***************************************************************************
     * *
     * Private fields                                                          *
     * *
     */

    /***************************************************************************
     * *
     * Public API                                                              *
     * *
     */

    /** {@inheritDoc}  */
    override val inputMap: InputMap<T>

    // To support touch devices, we have to slightly modify this behavior, such
    // that selection only happens on mouse release, if only minimal dragging
    // has occurred.
    private var latePress = false


    protected abstract val cellContainer: Control // e.g. ListView
    protected abstract val selectionModel: MultipleSelectionModel<*>?
    protected abstract val focusModel: FocusModel<*>?

    protected val index: Int
        get() = if (node is IndexedCell<*>) (node as IndexedCell<*>).index else -1

    protected val isSelected: Boolean
        get() = node.isSelected


    init {

        // create a map for cell-specific mappings (this reuses the default
        // InputMap installed on the control, if it is non-null, allowing us to pick up any user-specified mappings)
        inputMap = createInputMap()

        // TODO add focus traversal mappings (?)

        val pressedMapping = InputMap.MouseMapping(MouseEvent.MOUSE_PRESSED, EventHandler<MouseEvent> { this.mousePressed(it) })
        val releasedMapping = InputMap.MouseMapping(MouseEvent.MOUSE_RELEASED, EventHandler<MouseEvent> { this.mouseReleased(it) })
        val mouseDragged = InputMap.MouseMapping(MouseEvent.MOUSE_DRAGGED, EventHandler<MouseEvent> { this.mouseDragged(it) })
        addDefaultMapping(pressedMapping, releasedMapping, mouseDragged)
        pressedMapping.isAutoConsume = false
        releasedMapping.isAutoConsume = false
        mouseDragged.isAutoConsume = false
    }

    protected abstract fun edit(cell: T?)
    protected fun handleDisclosureNode(x: Double, y: Double): Boolean {
        return false
    }

    protected fun isClickPositionValid(x: Double, y: Double): Boolean {
        return true
    }

    fun mousePressed(e: MouseEvent) {
        if (e.isSynthesized) {
            latePress = true
        } else {
            latePress = isSelected
            if (!latePress) {
                doSelect(e.x, e.y, e.button, e.clickCount,
                        e.isShiftDown, e.isShortcutDown)
            }
        }
    }

    fun mouseReleased(e: MouseEvent) {
        if (latePress) {
            latePress = false
            doSelect(e.x, e.y, e.button, e.clickCount,
                    e.isShiftDown, e.isShortcutDown)
        }
    }

    fun mouseDragged(e: MouseEvent) {
        latePress = false
    }


    /***************************************************************************
     * *
     * Private implementation                                                  *
     * *
     */

    protected fun doSelect(x: Double, y: Double, button: MouseButton,
                           clickCount: Int, shiftDown: Boolean, shortcutDown: Boolean) {
        // we update the cell to point to the new tree node
        val cell = node

        val cellContainer = cellContainer

        // If the mouse event is not contained within this TreeCell, then
        // we don't want to react to it.
        if (cell.isEmpty || !cell.contains(x, y)) {
            return
        }

        val index = index
        val selected = cell.isSelected
        val sm = selectionModel ?: return

        val fm = focusModel ?: return

        // if the user has clicked on the disclosure node, we do nothing other
        // than expand/collapse the tree item (if applicable). We do not do editing!
        if (handleDisclosureNode(x, y)) {
            return
        }

        // we only care about clicks in certain places (depending on the subclass)
        if (!isClickPositionValid(x, y)) return

        // if shift is down, and we don't already have the initial focus index
        // recorded, we record the focus index now so that subsequent shift+clicks
        // result in the correct selection occuring (whilst the focus index moves
        // about).
        if (shiftDown) {
            if (!hasNonDefaultAnchor(cellContainer)) {
                setAnchor(cellContainer, fm.focusedIndex, false)
            }
        } else {
            removeAnchor(cellContainer)
        }

        if (button == MouseButton.PRIMARY || button == MouseButton.SECONDARY && !selected) {
            if (sm.selectionMode == SelectionMode.SINGLE) {
                simpleSelect(button, clickCount, shortcutDown)
            } else {
                if (shortcutDown) {
                    if (selected) {
                        // we remove this row from the current selection
                        sm.clearSelection(index)
                        fm.focus(index)
                    } else {
                        // We add this row to the current selection
                        sm.select(index)
                    }
                } else if (shiftDown && clickCount == 1) {
                    // we add all rows between the current selection focus and
                    // this row (inclusive) to the current selection.
                    val focusedIndex = getAnchor(cellContainer, fm.focusedIndex)

                    selectRows(focusedIndex, index)

                    fm.focus(index)
                } else {
                    simpleSelect(button, clickCount, shortcutDown)
                }
            }
        }
    }

    protected fun simpleSelect(button: MouseButton, clickCount: Int, shortcutDown: Boolean) {
        val index = index
        val sm = selectionModel
        var isAlreadySelected = sm!!.isSelected(index)

        if (isAlreadySelected && shortcutDown) {
            sm.clearSelection(index)
            focusModel!!.focus(index)
            isAlreadySelected = false
        } else {
            sm.clearAndSelect(index)
        }

        handleClicks(button, clickCount, isAlreadySelected)
    }

    protected fun handleClicks(button: MouseButton, clickCount: Int, isAlreadySelected: Boolean) {
        // handle editing, which only occurs with the primary mouse button
        if (button == MouseButton.PRIMARY) {
            if (clickCount == 1 && isAlreadySelected) {
                edit(node)
            } else if (clickCount == 1) {
                // cancel editing
                edit(null)
            } else if (clickCount == 2 && node.isEditable) {
                edit(node)
            }
        }
    }

    internal fun selectRows(focusedIndex: Int, index: Int) {
        val asc = focusedIndex < index

        // and then determine all row and columns which must be selected
        val minRow = Math.min(focusedIndex, index)
        val maxRow = Math.max(focusedIndex, index)

        // To prevent RT-32119, we make a copy of the selected indices
        // list first, so that we are not iterating and modifying it
        // concurrently.
        val selectedIndices = ArrayList(selectionModel!!.selectedIndices)
        var i = 0
        val max = selectedIndices.size
        while (i < max) {
            val selectedIndex = selectedIndices[i]
            if (selectedIndex < minRow || selectedIndex > maxRow) {
                selectionModel!!.clearSelection(selectedIndex)
            }
            i++
        }

        if (minRow == maxRow) {
            // RT-32560: This prevents the anchor 'sticking' in
            // the wrong place when a range is selected and then
            // selection goes back to the anchor position.
            // (Refer to the video in RT-32560 for more detail).
            selectionModel!!.select(minRow)
        } else {
            // RT-21444: We need to put the range in the correct
            // order or else the last selected row will not be the
            // last item in the selectedItems list of the selection
            // model,
            if (asc) {
                selectionModel!!.selectRange(minRow, maxRow + 1)
            } else {
                selectionModel!!.selectRange(maxRow, minRow - 1)
            }
        }
    }

    companion object {


        /***************************************************************************
         * *
         * Private static implementation                                           *
         * *
         */

        private val ANCHOR_PROPERTY_KEY = "anchor"

        // The virtualised controls all start with selection on row 0 by default.
        // This means that we have a default anchor, but it should be removed if
        // a different anchor could be set - and normally we ignore the default
        // anchor anyway.
        private val IS_DEFAULT_ANCHOR_KEY = "isDefaultAnchor"

        fun <T> getAnchor(control: Control, defaultResponse: T): T {
            return if (hasNonDefaultAnchor(control))
                control.properties[ANCHOR_PROPERTY_KEY] as T
            else
                defaultResponse
        }

        fun <T> setAnchor(control: Control?, anchor: T?, isDefaultAnchor: Boolean) {
            if (control == null) return
            if (anchor == null) {
                removeAnchor(control)
            } else {
                control.properties[ANCHOR_PROPERTY_KEY] = anchor
                control.properties[IS_DEFAULT_ANCHOR_KEY] = isDefaultAnchor
            }
        }

        fun hasNonDefaultAnchor(control: Control): Boolean {
            val isDefaultAnchor = control.properties.remove(IS_DEFAULT_ANCHOR_KEY)
            return (isDefaultAnchor == null || isDefaultAnchor == false) && hasAnchor(control)
        }

        fun hasDefaultAnchor(control: Control): Boolean {
            val isDefaultAnchor = control.properties.remove(IS_DEFAULT_ANCHOR_KEY)
            return isDefaultAnchor != null && isDefaultAnchor == true && hasAnchor(control)
        }

        private fun hasAnchor(control: Control): Boolean {
            return control.properties[ANCHOR_PROPERTY_KEY] != null
        }

        fun removeAnchor(control: Control) {
            control.properties.remove(ANCHOR_PROPERTY_KEY)
            control.properties.remove(IS_DEFAULT_ANCHOR_KEY)
        }
    }
}
