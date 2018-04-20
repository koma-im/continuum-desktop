package koma.gui.element.scene.traversal

import javafx.geometry.NodeOrientation

/**
 * Specifies the direction of traversal.
 */
enum class Direction private constructor(val isForward: Boolean) {

    UP(false),
    DOWN(true),
    LEFT(false),
    RIGHT(true),
    NEXT(true),
    NEXT_IN_LINE(true), // Like NEXT, but does not traverse into the current parent
    PREVIOUS(false);

    /**
     * Returns the direction with respect to the node's orientation. It affect's only arrow keys however, so it's not
     * an error to ignore this call if handling only next/previous traversal.
     * @param orientation
     * @return
     */
    fun getDirectionForNodeOrientation(orientation: NodeOrientation): Direction {
        if (orientation == NodeOrientation.RIGHT_TO_LEFT) {
            when (this) {
                LEFT -> return RIGHT
                RIGHT -> return LEFT
            }
        }
        return this
    }
}
