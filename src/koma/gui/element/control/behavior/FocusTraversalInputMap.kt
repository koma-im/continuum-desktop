package koma.gui.element.control.behavior

import javafx.scene.Node
import javafx.scene.input.KeyCode.*
import javafx.scene.input.KeyEvent
import koma.gui.element.control.inputmap.InputMap
import koma.gui.element.control.inputmap.KeyBinding
import koma.gui.element.scene.traversal.Direction
import java.util.*

object FocusTraversalInputMap
{
    private val mappings = ArrayList<InputMap.Mapping<*>>()

    init {
        mappings.add(InputMap.KeyMapping(UP) { e -> traverseUp(e) })
        mappings.add(InputMap.KeyMapping(DOWN) { e -> traverseDown(e) })
        mappings.add(InputMap.KeyMapping(LEFT) { e -> traverseLeft(e) })
        mappings.add(InputMap.KeyMapping(RIGHT) { e -> traverseRight(e) })
        mappings.add(InputMap.KeyMapping(TAB) { e -> traverseNext(e) })
        mappings.add(InputMap.KeyMapping(KeyBinding(TAB).shift()) { e -> traversePrevious(e) })

        mappings.add(InputMap.KeyMapping(KeyBinding(UP).shift().alt().ctrl()) { e -> traverseUp(e) })
        mappings.add(InputMap.KeyMapping(KeyBinding(DOWN).shift().alt().ctrl()) { e -> traverseDown(e) })
        mappings.add(InputMap.KeyMapping(KeyBinding(LEFT).shift().alt().ctrl()) { e -> traverseLeft(e) })
        mappings.add(InputMap.KeyMapping(KeyBinding(RIGHT).shift().alt().ctrl()) { e -> traverseRight(e) })
        mappings.add(InputMap.KeyMapping(KeyBinding(TAB).shift().alt().ctrl()) { e -> traverseNext(e) })
        mappings.add(InputMap.KeyMapping(KeyBinding(TAB).alt().ctrl()) { e -> traversePrevious(e) })
    }

    val focusTraversalMappings: Array<InputMap.Mapping<*>>
        get() = mappings.toTypedArray()

    fun <N : Node> createInputMap(node: N): InputMap<N> {
        val inputMap = InputMap(node)
        inputMap.mappings.addAll(*focusTraversalMappings)
        return inputMap
    }


    /***************************************************************************
     * Focus Traversal methods                                                 *
     */

    /**
     * Called by any of the BehaviorBase traverse methods to actually effect a
     * traversal of the focus. The default behavior of this method is to simply
     * traverse on the given node, passing the given direction. A
     * subclass may override this method.
     *
     * @param node The node to traverse on
     * @param dir The direction to traverse
     */
    fun traverse(node: Node?, dir: Direction) {
        if (node == null) {
            throw IllegalArgumentException("Attempting to traverse on a null Node. " + "Most probably a KeyEvent has been fired with a null target specified.")
        }
        //NodeHelper.traverse(node, dir);
    }

    /**
     * Calls the focus traversal engine and indicates that traversal should
     * go the next focusTraversable Node above the current one.
     */
    fun traverseUp(e: KeyEvent) {
        traverse(getNode(e), Direction.UP)
    }

    /**
     * Calls the focus traversal engine and indicates that traversal should
     * go the next focusTraversable Node below the current one.
     */
    fun traverseDown(e: KeyEvent) {
        traverse(getNode(e), Direction.DOWN)
    }

    /**
     * Calls the focus traversal engine and indicates that traversal should
     * go the next focusTraversable Node left of the current one.
     */
    fun traverseLeft(e: KeyEvent) {
        traverse(getNode(e), Direction.LEFT)
    }

    /**
     * Calls the focus traversal engine and indicates that traversal should
     * go the next focusTraversable Node right of the current one.
     */
    fun traverseRight(e: KeyEvent) {
        traverse(getNode(e), Direction.RIGHT)
    }

    /**
     * Calls the focus traversal engine and indicates that traversal should
     * go the next focusTraversable Node in the focus traversal cycle.
     */
    fun traverseNext(e: KeyEvent) {
        traverse(getNode(e), Direction.NEXT)
    }

    /**
     * Calls the focus traversal engine and indicates that traversal should
     * go the previous focusTraversable Node in the focus traversal cycle.
     */
    fun traversePrevious(e: KeyEvent) {
        traverse(getNode(e), Direction.PREVIOUS)
    }

    private fun getNode(e: KeyEvent): Node? {
        val target = e.target
        return target as? Node
    }
}
