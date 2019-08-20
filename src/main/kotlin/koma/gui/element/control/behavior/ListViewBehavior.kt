package koma.gui.element.control.behavior

import javafx.beans.value.ChangeListener
import javafx.beans.value.WeakChangeListener
import javafx.collections.ListChangeListener
import javafx.collections.ObservableList
import javafx.collections.WeakListChangeListener
import javafx.event.EventHandler
import javafx.scene.control.ListView
import javafx.scene.control.MultipleSelectionModel
import javafx.scene.control.SelectionMode
import javafx.scene.input.KeyCode.*
import javafx.scene.input.KeyEvent
import javafx.scene.input.MouseEvent
import koma.gui.element.control.Utils
import koma.gui.element.control.behavior.FocusTraversalInputMap.createInputMap
import koma.gui.element.control.inputmap.KInputMap
import koma.gui.element.control.inputmap.KeyBinding
import koma.gui.element.control.inputmap.MappingType
import koma.gui.element.control.inputmap.mapping.KeyMapping
import koma.gui.element.control.inputmap.mapping.MouseMapping
import mu.KotlinLogging
import java.util.*
import java.util.function.Predicate

private val logger = KotlinLogging.logger {}

class ListViewBehavior<T>(val node: ListView<T>) {
    private val installedDefaultMappings = mutableListOf<MappingType>()
    private val childInputMapDisposalHandlers = mutableListOf<Runnable>()
    private val inputMap: KInputMap<ListView<T>> = createInputMap(node)

    private val keyEventListener = { e: KeyEvent ->
        if (!e.isConsumed) {
            // RT-12751: we want to keep an eye on the user holding down the shift key,
            // so that we know when they enter/leave multiple selection mode. This
            // changes what happens when certain key combinations are pressed.
            isShiftDown = e.eventType == KeyEvent.KEY_PRESSED && e.isShiftDown
            isShortcutDown = e.eventType == KeyEvent.KEY_PRESSED && e.isShortcutDown
        }
    }


    /**************************************************************************
     * State and Functions                            *
     */

    private var isShiftDown = false
    private var isShortcutDown = false

    var onScrollPageUp: ((KeyEvent)->Unit)? = null
    var onScrollPageDown: ((KeyEvent)->Unit)? = null
    private var onFocusPreviousRow: Runnable? = null
    private var onFocusNextRow: Runnable? = null
    private var onSelectPreviousRow: Runnable? = null
    private var onSelectNextRow: Runnable? = null
    private var onMoveToFirstCell: Runnable? = null
    private var onMoveToLastCell: Runnable? = null

    private var selectionChanging = false

    private fun selectedIndicesChange(c: ListChangeListener.Change<out Int>) {
        var newAnchor = anchor

        while (c.next()) {
            if (c.wasReplaced()) {
                if (CellBehaviorBase.hasDefaultAnchor(node)) {
                    CellBehaviorBase.removeAnchor(node)
                    continue
                }
            }

            val shift = if (c.wasPermutated()) c.to - c.from else 0

            val sm = node.selectionModel

            // there are no selected items, so lets clear out the anchor
            if (!selectionChanging) {
                if (sm.isEmpty) {
                    newAnchor = -1
                } else if (hasAnchor() && !sm.isSelected(anchor + shift)) {
                    newAnchor = -1
                }
            }

            // we care about the situation where the selection changes, and there is no anchor. In this
            // case, we set a new anchor to be the selected index
            if (newAnchor == -1) {
                val addedSize = c.addedSize
                newAnchor = if (addedSize > 0) c.addedSubList[addedSize - 1] else newAnchor
            }
        }

        if (newAnchor > -1) {
            anchor = newAnchor
        }
    }
    private val selectedIndicesListener = ListChangeListener<Int> { selectedIndicesChange(it) }

    private fun itemsListChange(c: ListChangeListener.Change<out T>) {
        while (c.next()) {
            if (!hasAnchor()) continue

            var newAnchor = if (hasAnchor()) anchor else 0

            if (c.wasAdded() && c.from <= newAnchor) {
                newAnchor += c.addedSize
            } else if (c.wasRemoved() && c.from <= newAnchor) {
                newAnchor -= c.removedSize
            }

            anchor = if (newAnchor < 0) 0 else newAnchor
        }
    }
    private val itemsListListener = ListChangeListener<T> { itemsListChange(it) }

    private val itemsListener = ChangeListener<ObservableList<T>> { _, oldValue, newValue ->
        oldValue?.removeListener(weakItemsListListener)
        newValue?.addListener(weakItemsListListener)
    }

    private val selectionModelListener = ChangeListener<MultipleSelectionModel<T>> { _, oldValue, newValue ->
        oldValue?.selectedIndices?.removeListener(weakSelectedIndicesListener)
        newValue?.selectedIndices?.addListener(weakSelectedIndicesListener)
    }

    private val weakItemsListener = WeakChangeListener(itemsListener)
    private val weakSelectedIndicesListener = WeakListChangeListener<Int>(selectedIndicesListener)
    private val weakItemsListListener = WeakListChangeListener<T>(itemsListListener)
    private val weakSelectionModelListener = WeakChangeListener(selectionModelListener)

    private var anchor: Int
        get() = CellBehaviorBase.getAnchor<Int>(node, node.focusModel.focusedIndex)
        set(anchor) {
            CellBehaviorBase.setAnchor<Int>(node, if (anchor < 0) null else anchor, false)
        }

    private val rowCount: Int
        get() = if (node.items == null) 0 else node.items.size


    init {

        // create a map for listView-specific mappings
        val control = node
        // add focus traversal mappings
        addDefaultMapping(inputMap, FocusTraversalInputMap.mappings.map { MappingType.Key(it) })
        addDefaultMapping(inputMap, listOf(
                KeyMapping(HOME, {
                    logger.debug { "ListViewBehavior<T> KeyMapping HOME selectFirstRow" }
                    selectFirstRow()
                }),
                KeyMapping(END, {
                    logger.debug { "ListViewBehavior<T> KeyMapping END selectLastRow" }
                    selectLastRow()
                }),
                KeyMapping(KeyBinding(HOME).shift(), { _ -> selectAllToFirstRow() }),
                KeyMapping(KeyBinding(END).shift(), { _ -> selectAllToLastRow() }),

                KeyMapping(KeyBinding(SPACE).shift(), { _ -> selectAllToFocus() }),
                KeyMapping(KeyBinding(SPACE).shortcut().shift(), { _ -> selectAllToFocus() }),

                KeyMapping(PAGE_UP, {
                    onScrollPageUp?.invoke(it)
                }),
                KeyMapping(PAGE_DOWN, {
                    onScrollPageDown?.invoke(it)
                }),

                KeyMapping(ENTER, { _ -> activate() }),
                KeyMapping(SPACE, { _ -> activate() }),
                KeyMapping(F2, { _ -> activate() }),
                KeyMapping(ESCAPE, { _ -> cancelEdit() }),

                KeyMapping(KeyBinding(A).shortcut(), { _ -> selectAll() }),
                KeyMapping(KeyBinding(HOME).shortcut(), { _ -> focusFirstRow() }),
                KeyMapping(KeyBinding(END).shortcut(), { _ -> focusLastRow() }),

                KeyMapping(KeyBinding(BACK_SLASH).shortcut(), { _ -> clearSelection() })
        ).map { MappingType.Key(it) })

        addDefaultMapping(inputMap, MouseMapping(MouseEvent.MOUSE_PRESSED, EventHandler { this.mousePressed(it) }))

        // create OS-specific child mappings
        // --- mac OS
        val macInputMap = KInputMap<ListView<T>>(control)
        macInputMap.interceptor = Predicate { _ -> !Utils.MAC }
        addDefaultMapping(macInputMap, KeyMapping(KeyBinding(SPACE).shortcut().ctrl(), { _ -> toggleFocusOwnerSelection() }))
        addDefaultChildMap(inputMap, macInputMap)

        // --- all other platforms
        val otherOsInputMap = KInputMap<ListView<T>>(control)
        otherOsInputMap.interceptor = Predicate{ _ -> Utils.MAC }
        addDefaultMapping(otherOsInputMap, KeyMapping(KeyBinding(SPACE).ctrl(), { _ -> toggleFocusOwnerSelection() }))
        addDefaultChildMap(inputMap, otherOsInputMap)

        // create child map for vertical listview
        val verticalListInputMap = KInputMap<ListView<T>>(control)

        addDefaultKeyMapping(verticalListInputMap, listOf(
                KeyMapping(UP, { _ -> selectPreviousRow() }),
                KeyMapping(KP_UP, { _ -> selectPreviousRow() }),
                KeyMapping(DOWN, { _ -> selectNextRow() }),
                KeyMapping(KP_DOWN, { _ -> selectNextRow() }),

                KeyMapping(KeyBinding(UP).shift(), { _ -> alsoSelectPreviousRow() }),
                KeyMapping(KeyBinding(KP_UP).shift(), { _ -> alsoSelectPreviousRow() }),
                KeyMapping(KeyBinding(DOWN).shift(), { _ -> alsoSelectNextRow() }),
                KeyMapping(KeyBinding(KP_DOWN).shift(), { _ -> alsoSelectNextRow() }),

                KeyMapping(KeyBinding(UP).shortcut(), { _ -> focusPreviousRow() }),
                KeyMapping(KeyBinding(DOWN).shortcut(), { _ -> focusNextRow() }),

                KeyMapping(KeyBinding(UP).shortcut().shift(), { _ -> discontinuousSelectPreviousRow() }),
                KeyMapping(KeyBinding(DOWN).shortcut().shift(), { _ -> discontinuousSelectNextRow() }),
                KeyMapping(KeyBinding(HOME).shortcut().shift(), { _ -> discontinuousSelectAllToFirstRow() }),
                KeyMapping(KeyBinding(END).shortcut().shift(), { _ -> discontinuousSelectAllToLastRow() })
        ))

        addDefaultChildMap(inputMap, verticalListInputMap)

        // set up other listeners
        // We make this an event _filter_ so that we can determine the state
        // of the shift key before the event handlers get a shot at the event.
        control.addEventFilter(KeyEvent.ANY, keyEventListener)

        control.itemsProperty().addListener(weakItemsListener)
        if (control.items != null) {
            control.items.addListener(weakItemsListListener)
        }

        // Fix for RT-16565
        control.selectionModelProperty().addListener(weakSelectionModelListener)
        if (control.selectionModel != null) {
            control.selectionModel.selectedIndices.addListener(weakSelectedIndicesListener)
        }
    }

     fun dispose() {
        val control = node

        CellBehaviorBase.removeAnchor(control)

        control.removeEventHandler(KeyEvent.ANY, keyEventListener)
    }

    fun setOnFocusPreviousRow(r: Runnable) {
        onFocusPreviousRow = r
    }

    fun setOnFocusNextRow(r: Runnable) {
        onFocusNextRow = r
    }

    fun setOnSelectPreviousRow(r: Runnable) {
        onSelectPreviousRow = r
    }

    fun setOnSelectNextRow(r: Runnable) {
        onSelectNextRow = r
    }

    fun setOnMoveToFirstCell(r: Runnable) {
        onMoveToFirstCell = r
    }

    fun setOnMoveToLastCell(r: Runnable) {
        onMoveToLastCell = r
    }

    private fun hasAnchor(): Boolean {
        return CellBehaviorBase.hasNonDefaultAnchor(node)
    }

    private fun mousePressed(e: MouseEvent) {
        if (!e.isShiftDown && !e.isSynthesized) {
            val index = node.selectionModel.selectedIndex
            anchor = index
        }

        if (!node.isFocused && node.isFocusTraversable) {
            node.requestFocus()
        }
    }

    private fun clearSelection() {
        node.selectionModel.clearSelection()
    }

    private fun focusFirstRow() {
        val fm = node.focusModel ?: return
        fm.focus(0)

        if (onMoveToFirstCell != null) onMoveToFirstCell!!.run()
    }

    private fun focusLastRow() {
        val fm = node.focusModel ?: return
        fm.focus(rowCount - 1)

        if (onMoveToLastCell != null) onMoveToLastCell!!.run()
    }

    private fun focusPreviousRow() {
        val fm = node.focusModel ?: return

        node.selectionModel ?: return

        fm.focusPrevious()

        if (!isShortcutDown || anchor == -1) {
            anchor = fm.focusedIndex
        }

        if (onFocusPreviousRow != null) onFocusPreviousRow!!.run()
    }

    private fun focusNextRow() {
        val fm = node.focusModel ?: return

        node.selectionModel ?: return

        fm.focusNext()

        if (!isShortcutDown || anchor == -1) {
            anchor = fm.focusedIndex
        }

        if (onFocusNextRow != null) onFocusNextRow!!.run()
    }

    private fun alsoSelectPreviousRow() {
        val fm = node.focusModel ?: return

        val sm = node.selectionModel ?: return

        if (isShiftDown && anchor != -1) {
            val newRow = fm.focusedIndex - 1
            if (newRow < 0) return

            var anchor = anchor

            if (!hasAnchor()) {
                anchor = fm.focusedIndex
            }

            if (sm.selectedIndices.size > 1) {
                clearSelectionOutsideRange(anchor, newRow)
            }

            if (anchor > newRow) {
                sm.selectRange(anchor, newRow - 1)
            } else {
                sm.selectRange(anchor, newRow + 1)
            }
        } else {
            sm.selectPrevious()
        }

        onSelectPreviousRow!!.run()
    }

    private fun alsoSelectNextRow() {
        val fm = node.focusModel ?: return

        val sm = node.selectionModel ?: return

        if (isShiftDown && anchor != -1) {
            val newRow = fm.focusedIndex + 1
            var anchor = anchor

            if (!hasAnchor()) {
                anchor = fm.focusedIndex
            }

            if (sm.selectedIndices.size > 1) {
                clearSelectionOutsideRange(anchor, newRow)
            }

            if (anchor > newRow) {
                sm.selectRange(anchor, newRow - 1)
            } else {
                sm.selectRange(anchor, newRow + 1)
            }
        } else {
            sm.selectNext()
        }

        onSelectNextRow!!.run()
    }

    private fun clearSelectionOutsideRange(start: Int, end: Int) {
        val sm = node.selectionModel ?: return

        val min = Math.min(start, end)
        val max = Math.max(start, end)

        val indices = ArrayList(sm.selectedIndices)

        selectionChanging = true
        for (i in indices.indices) {
            val index = indices[i]
            if (index < min || index > max) {
                sm.clearSelection(index)
            }
        }
        selectionChanging = false
    }

    private fun selectPreviousRow() {
        val fm = node.focusModel ?: return

        val focusIndex = fm.focusedIndex
        if (focusIndex <= 0) {
            return
        }

        anchor = focusIndex - 1
        node.selectionModel.clearAndSelect(focusIndex - 1)
        onSelectPreviousRow!!.run()
    }

    private fun selectNextRow() {
        val listView = node
        val fm = listView.focusModel ?: return

        val focusIndex = fm.focusedIndex
        if (focusIndex == rowCount - 1) {
            return
        }

        val sm = listView.selectionModel ?: return

        anchor = focusIndex + 1
        sm.clearAndSelect(focusIndex + 1)
        if (onSelectNextRow != null) onSelectNextRow!!.run()
    }

    private fun selectFirstRow() {
        if (rowCount > 0) {
            node.selectionModel.clearAndSelect(0)
            if (onMoveToFirstCell != null) onMoveToFirstCell!!.run()
        }
    }

    private fun selectLastRow() {
        node.selectionModel.clearAndSelect(rowCount - 1)
        if (onMoveToLastCell != null) onMoveToLastCell!!.run()
    }

    private fun selectAllToFirstRow() {
        val sm = node.selectionModel ?: return

        val fm = node.focusModel ?: return

        var leadIndex = fm.focusedIndex

        if (isShiftDown) {
            leadIndex = if (hasAnchor()) anchor else leadIndex
        }

        sm.clearSelection()
        sm.selectRange(leadIndex, -1)

        // RT-18413: Focus must go to first row
        fm.focus(0)

        if (isShiftDown) {
            anchor = leadIndex
        }

        if (onMoveToFirstCell != null) onMoveToFirstCell!!.run()
    }

    private fun selectAllToLastRow() {
        val sm = node.selectionModel ?: return

        val fm = node.focusModel ?: return

        var leadIndex = fm.focusedIndex

        if (isShiftDown) {
            leadIndex = if (hasAnchor()) anchor else leadIndex
        }

        sm.clearSelection()
        sm.selectRange(leadIndex, rowCount)

        if (isShiftDown) {
            anchor = leadIndex
        }

        if (onMoveToLastCell != null) onMoveToLastCell!!.run()
    }

    private fun selectAll() {
        val sm = node.selectionModel ?: return
        sm.selectAll()
    }

    private fun selectAllToFocus() {
        // Fix for RT-31241
        val listView = node
        if (listView.editingIndex >= 0) return

        val sm = listView.selectionModel ?: return

        val fm = listView.focusModel ?: return

        val focusIndex = fm.focusedIndex
        val anchor = anchor

        sm.clearSelection()
        val startPos = anchor
        val endPos = if (anchor > focusIndex) focusIndex - 1 else focusIndex + 1
        sm.selectRange(startPos, endPos)
        // anchor = if (setAnchorToFocusIndex) focusIndex else anchor
    }

    private fun cancelEdit() {
        node.edit(-1)
    }

    private fun activate() {
        val focusedIndex = node.focusModel.focusedIndex
        node.selectionModel.select(focusedIndex)
        anchor = focusedIndex

        // edit this row also
        if (focusedIndex >= 0) {
            node.edit(focusedIndex)
        }
    }

    private fun toggleFocusOwnerSelection() {
        val sm = node.selectionModel ?: return

        val fm = node.focusModel ?: return

        val focusedIndex = fm.focusedIndex

        if (sm.isSelected(focusedIndex)) {
            sm.clearSelection(focusedIndex)
            fm.focus(focusedIndex)
        } else {
            sm.select(focusedIndex)
        }

        anchor = focusedIndex
    }

    /**************************************************************************
     * Discontinuous Selection                                                *
     */

    private fun discontinuousSelectPreviousRow() {
        val sm = node.selectionModel ?: return

        if (sm.selectionMode != SelectionMode.MULTIPLE) {
            selectPreviousRow()
            return
        }

        val fm = node.focusModel ?: return

        val focusIndex = fm.focusedIndex
        val newFocusIndex = focusIndex - 1
        if (newFocusIndex < 0) return

        var startIndex = focusIndex
        if (isShiftDown) {
            startIndex = if (anchor == -1) focusIndex else anchor
        }

        sm.selectRange(newFocusIndex, startIndex + 1)
        fm.focus(newFocusIndex)

        if (onFocusPreviousRow != null) onFocusPreviousRow!!.run()
    }

    private fun discontinuousSelectNextRow() {
        val sm = node.selectionModel ?: return

        if (sm.selectionMode != SelectionMode.MULTIPLE) {
            selectNextRow()
            return
        }

        val fm = node.focusModel ?: return

        val focusIndex = fm.focusedIndex
        val newFocusIndex = focusIndex + 1
        if (newFocusIndex >= rowCount) return

        var startIndex = focusIndex
        if (isShiftDown) {
            startIndex = if (anchor == -1) focusIndex else anchor
        }

        sm.selectRange(startIndex, newFocusIndex + 1)
        fm.focus(newFocusIndex)

        if (onFocusNextRow != null) onFocusNextRow!!.run()
    }

    private fun discontinuousSelectAllToFirstRow() {
        val sm = node.selectionModel ?: return

        val fm = node.focusModel ?: return

        val index = fm.focusedIndex
        sm.selectRange(0, index)
        fm.focus(0)

        if (onMoveToFirstCell != null) onMoveToFirstCell!!.run()
    }

    private fun discontinuousSelectAllToLastRow() {
        val sm = node.selectionModel ?: return

        val fm = node.focusModel ?: return

        val index = fm.focusedIndex + 1
        sm.selectRange(index, rowCount)

        if (onMoveToLastCell != null) onMoveToLastCell!!.run()
    }

    protected fun addDefaultKeyMapping(i: KInputMap<ListView<T>>, newMapping: List<KeyMapping>) {
        addDefaultMapping(i, newMapping.map { MappingType.Key(it) })
    }

    protected fun addDefaultMapping(i: KInputMap<ListView<T>>, newMapping: KeyMapping) {
        addDefaultMapping(i, listOf(MappingType.Key(newMapping)))
    }

    protected fun addDefaultMapping(i: KInputMap<ListView<T>>, newMapping: MouseMapping) {
        addDefaultMapping(i, listOf(MappingType.Mouse(newMapping)))
    }

    protected fun addDefaultMapping(inputMap: KInputMap<ListView<T>>, newMapping: List<MappingType>) {
        // make a copy of the existing mappings, so we only check against those
        val existingMappings = inputMap.mappings.toList()

        for (mapping in newMapping) {
            // check if a mapping already exists, and if so, do not add this mapping
            // TODO this is insufficient as we need to check entire InputMap hierarchy
            if (existingMappings.contains(mapping)) continue

            inputMap.mappings.add(mapping)
            installedDefaultMappings.add(mapping)
        }
    }

    protected fun addDefaultChildMap(
            parentInputMap: KInputMap<ListView<T>>,
            newChildInputMap: KInputMap<ListView<T>>) {
        parentInputMap.childInputMaps.add(newChildInputMap)

        childInputMapDisposalHandlers.add(Runnable{ parentInputMap.childInputMaps.remove(newChildInputMap) })
    }
}
