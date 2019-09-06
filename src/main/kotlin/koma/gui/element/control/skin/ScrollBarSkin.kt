package koma.gui.element.control.skin

import com.sun.javafx.scene.control.Properties
import com.sun.javafx.util.Utils
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import de.jensd.fx.glyphs.fontawesome.utils.FontAwesomeIconFactory
import javafx.geometry.Insets
import javafx.geometry.Orientation
import javafx.geometry.Point2D
import javafx.geometry.VPos
import javafx.scene.AccessibleAttribute
import javafx.scene.AccessibleRole
import javafx.scene.control.Control
import javafx.scene.control.ScrollBar
import javafx.scene.control.SkinBase
import javafx.scene.effect.DropShadow
import javafx.scene.input.MouseButton
import javafx.scene.input.ScrollEvent
import javafx.scene.layout.*
import javafx.scene.paint.Color
import javafx.scene.shape.Circle
import koma.gui.element.control.KScrollBarBehavior
import koma.gui.element.control.KVirtualScrollBar
import mu.KotlinLogging
import kotlin.math.abs

private val logger = KotlinLogging.logger {}

/**
 * Creates a new ScrollBarSkin instance, installing the necessary child
 * nodes into the Control [children][Control.getChildren] list, as
 * well as the necessary input mappings for handling key, mouse, etc events.
 *
 * @param control The control that this skin should be installed onto.
 */
class ScrollBarSkin(
        control: KVirtualScrollBar<*, *>,
        /**
         * the width of the scrollbar
         */
        breath: Double? = null
) : SkinBase<ScrollBar>(control) {

    /*
     * the breadth of the scrollbar. The "breadth" is the distance
     * across the scrollbar, i.e. if vertical the width.
     */
    private val breadth: Double = breath?: Properties.DEFAULT_EMBEDDED_SB_BREADTH+ snappedLeftInset() + snappedRightInset()

    private val behavior = KScrollBarBehavior(control)

    private val thumb= object : StackPane() {
        override fun queryAccessibleAttribute(attribute: AccessibleAttribute, vararg parameters: Any): Any {
            when (attribute) {
                AccessibleAttribute.VALUE -> return skinnable.value
                else -> return super.queryAccessibleAttribute(attribute, *parameters)
            }
        }
    }
    private val track = StackPane()
    /**
     * click scroll to bottom
     */
    private val bottomButton =Pane().apply {
        val radius = breadth * 2.5
        children.add(FontAwesomeIconFactory.get().createIcon(
                FontAwesomeIcon.ANGLE_DOWN, "${breadth*4}px"
        ).apply {
            opacity = .5
            this.textOrigin = VPos.CENTER
            translateX = radius / 2
            translateY = radius
        })
        effect = buttonShadow
        border = Border(BorderStroke(Color.GRAY, BorderStrokeStyle.SOLID, CornerRadii.EMPTY, BorderWidths.DEFAULT))
        shape = Circle(radius)
        background = Background(BackgroundFill(Color.WHITE, CornerRadii.EMPTY, Insets.EMPTY))
        prefHeight = radius * 2
        prefWidth = radius * 2
    }

    private var trackLength: Double = 0.0
    private var thumbLength: Double = 0.0

    private var preDragThumbPos: Double = 0.0
    private var dragStart: Point2D? = null // in the track's coord system



    init {
        initialize()
        skinnable.requestLayout()

        // Register listeners
        val consumer = { _: Any? ->
            positionThumb()
            showBottomButton()
            skinnable.requestLayout()
        }
        registerChangeListener(control.minProperty(), consumer)
        registerChangeListener(control.maxProperty(), consumer)
        registerChangeListener(control.visibleAmountProperty(), consumer)
        registerChangeListener(control.valueProperty()) { e ->
            positionThumb()
            showBottomButton()
        }
    }

    override fun dispose() {
        super.dispose()

        behavior.dispose()
    }

    override fun layoutChildren(x: Double, y: Double,
                                w: Double, h: Double) {

        val s = skinnable

        /*
          Compute the percentage length of thumb as (visibleAmount/range)
          if max isn't greater than min then there is nothing to do here
         */
        val visiblePortion: Double
        if (s.max > s.min) {
            visiblePortion = s.visibleAmount / (s.max - s.min)
        } else {
            visiblePortion = 1.0
        }

        val xs = snapPositionX(x)
        val ys = snapPositionY(y)
        val hs = snapSizeY(h)
        logger.trace { "layoutChildren $x $y $w $h $xs $ys" }
        trackLength = hs
        thumbLength = snapSizeY(Utils.clamp(minThumbLength(), trackLength * visiblePortion, trackLength))
        track.resizeRelocate(xs, ys, w, trackLength)
        thumb.resize(snapSizeX(if (x >= 0) w else w + x), thumbLength) // Account for negative padding
        bottomButton.apply {
            resize(prefWidth, prefHeight)
            val sep = breadth * 2
            relocate( xs -sep - prefWidth, ys + hs - sep - prefHeight)
        }
        positionThumb()

        // things should be invisible only when well below minimum length
        if ( h < computeMinHeight(
                        -1.0,
                        y.toInt().toDouble(),
                        snappedRightInset(),
                        snappedBottomInset(),
                        x.toInt().toDouble()
                ) - (y + snappedBottomInset())
        ) {
            track.isVisible = false
            thumb.isVisible = false
            bottomButton.isVisible = false
            return
        }
        track.isVisible = true
        thumb.isVisible = true

        showBottomButton()

    }

    /*
     * Minimum length is the length of the end buttons plus twice the
     * minimum thumb length, which should be enough for a reasonably-sized
     * track. Minimum breadth is determined by the breadths of the
     * end buttons.
     */
    override fun computeMinWidth(height: Double, topInset: Double, rightInset: Double, bottomInset: Double, leftInset: Double): Double {
        return breadth
    }

    override fun computeMinHeight(width: Double, topInset: Double, rightInset: Double, bottomInset: Double, leftInset: Double): Double {
        return   minTrackLength() + topInset + bottomInset
    }

    /*
     * Preferred size. The breadth is determined by the breadth of
     * the end buttons. The length is a constant default length.
     * Usually applications or other components will either set a
     * specific length using LayoutInfo or will stretch the length
     * of the scrollbar to fit a container.
     */
    override fun computePrefWidth(height: Double, topInset: Double, rightInset: Double, bottomInset: Double, leftInset: Double): Double {
        return breadth
    }

    override fun computePrefHeight(height: Double, topInset: Double, rightInset: Double, bottomInset: Double, leftInset: Double): Double {
        return Properties.DEFAULT_LENGTH.toDouble() + topInset + bottomInset

    }

    override fun computeMaxWidth(height: Double, topInset: Double, rightInset: Double, bottomInset: Double, leftInset: Double): Double {
        val s = skinnable
        return s.prefWidth(-1.0)

    }

    override fun computeMaxHeight(width: Double, topInset: Double, rightInset: Double, bottomInset: Double, leftInset: Double): Double {
        return Double.MAX_VALUE
    }

    /**
     * Initializes the ScrollBarSkin. Creates the scene and sets up all the
     * bindings for the group.
     */
    private fun initialize() {
        track!!.styleClass.setAll("track")
        thumb.background = Background(BackgroundFill(Color.DARKGREY, CornerRadii.EMPTY, Insets.EMPTY))
        thumb!!.accessibleRole = AccessibleRole.THUMB

        bottomButton.setOnMouseClicked {
            if (it.button == MouseButton.PRIMARY) {
                logger.debug { "go to bottom" }
                behavior.end()
                it.consume()
            }
        }
        track!!.setOnMousePressed { me ->
            if (!thumb!!.isPressed && me.button == MouseButton.PRIMARY) {
                if (trackLength != 0.0) {
                    behavior!!.trackPress(me.y / trackLength)
                    me.consume()
                }
            }
        }

        track!!.setOnMouseReleased { me ->
            behavior!!.trackRelease()
            me.consume()
        }

        thumb!!.setOnMousePressed { me ->
            if (me.isSynthesized) {
                // touch-screen events handled by Scroll handler
                me.consume()
                return@setOnMousePressed
            }
            /*
             ** if max isn't greater than min then there is nothing to do here
             */
            if (skinnable.max > skinnable.min) {
                dragStart = thumb!!.localToParent(me.x, me.y)
                val clampedValue = Utils.clamp(skinnable.min, skinnable.value, skinnable.max)
                preDragThumbPos = (clampedValue - skinnable.min) / (skinnable.max - skinnable.min)
                me.consume()
            }
        }


        thumb!!.setOnMouseDragged { me ->
            if (me.isSynthesized) {
                // touch-screen events handled by Scroll handler
                me.consume()
                return@setOnMouseDragged
            }
            /*
             ** if max isn't greater than min then there is nothing to do here
             */
            if (skinnable.max > skinnable.min) {
                /*
                 ** if the tracklength isn't greater then do nothing....
                 */
                if (trackLength > thumbLength) {
                    val cur = thumb!!.localToParent(me.x, me.y)
                    if (dragStart == null) {
                        // we're getting dragged without getting a mouse press
                        dragStart = thumb!!.localToParent(me.x, me.y)
                    }
                    val dragPos = if (skinnable.orientation == Orientation.VERTICAL) cur.y - dragStart!!.y else cur.x - dragStart!!.x
                    behavior!!.thumbDragged(preDragThumbPos + dragPos / (trackLength - thumbLength))
                }

                me.consume()
            }
        }

        thumb!!.setOnScrollStarted { se ->
            if (se.isDirect) {
                /*
                 ** if max isn't greater than min then there is nothing to do here
                 */
                if (skinnable.max > skinnable.min) {
                    dragStart = thumb!!.localToParent(se.x, se.y)
                    val clampedValue = Utils.clamp(skinnable.min, skinnable.value, skinnable.max)
                    preDragThumbPos = (clampedValue - skinnable.min) / (skinnable.max - skinnable.min)
                    se.consume()
                }
            }
        }

        thumb!!.setOnScroll { event ->
            if (event.isDirect) {
                /*
                 ** if max isn't greater than min then there is nothing to do here
                 */
                if (skinnable.max > skinnable.min) {
                    /*
                     ** if the tracklength isn't greater then do nothing....
                     */
                    if (trackLength > thumbLength) {
                        val cur = thumb!!.localToParent(event.x, event.y)
                        if (dragStart == null) {
                            // we're getting dragged without getting a mouse press
                            dragStart = thumb!!.localToParent(event.x, event.y)
                        }
                        val dragPos = if (skinnable.orientation == Orientation.VERTICAL) cur.y - dragStart!!.y else cur.x - dragStart!!.x
                        behavior!!.thumbDragged(/*todo*/preDragThumbPos + dragPos / (trackLength - thumbLength))
                    }

                    event.consume()
                    return@setOnScroll
                }
            }
        }


        skinnable.addEventHandler(ScrollEvent.SCROLL) { event ->
            /*
             ** if the tracklength isn't greater then do nothing....
             */
            if (trackLength > thumbLength) {

                var dx = event.deltaX
                val dy = event.deltaY

                /*
                 ** in 2.0 a horizontal scrollbar would scroll on a vertical
                 ** drag on a tracker-pad. We need to keep this behavior.
                 */
                dx = if (abs(dx) < abs(dy)) dy else dx

                /*
                 ** we only consume an event that we've used.
                 */
                val sb = skinnable as ScrollBar

                val delta = if (skinnable.orientation == Orientation.VERTICAL) dy else dx

                /*
                 ** RT-22941 - If this is either a touch or inertia scroll
                 ** then we move to the position of the touch point.
                 *
                 * TODO: this fix causes RT-23406 ([ScrollBar, touch] Dragging scrollbar from the
                 * track on touchscreen causes flickering)
                 */
                if (event.isDirect) {
                    if (trackLength > thumbLength) {
                        behavior!!.thumbDragged((if (skinnable.orientation == Orientation.VERTICAL) event.y else event.x) / trackLength)
                        event.consume()
                    }
                } else {
                    if (delta > 0.0 && sb.value > sb.min) {
                        sb.decrement()
                        event.consume()
                    } else if (delta < 0.0 && sb.value < sb.max) {
                        sb.increment()
                        event.consume()
                    }
                }
            }
        }

        children.clear()
        children.addAll(bottomButton)
        children.addAll(track, thumb)

    }

    internal fun minThumbLength(): Double {
        return 1.5f * breadth
    }

    internal fun minTrackLength(): Double {
        return 2.0f * breadth
    }

    /**
     * Called when ever either min, max or value changes, so thumb's layoutX, Y is recomputed.
     */
    internal fun positionThumb() {
        val s = skinnable
        val clampedValue = Utils.clamp(s.min, s.value, s.max)
        var trackPos: Double = if (s.max - s.min > 0) (trackLength - thumbLength) * (clampedValue - s.min) / (s.max - s.min) else 0.0

        thumb!!.translateX = snapPositionX(snappedLeftInset())
        thumb!!.translateY = snapPositionY(trackPos + snappedTopInset())
    }

    private fun showBottomButton(){
        val s = skinnable
        val pos: Double = if (s.max - s.min > 0) (s.value - s.min) / (s.max - s.min) else 1.0
        val showButt = pos < .95
        logger.trace { "pos $pos should show bottom button is $showButt" }
        bottomButton.isVisible = showButt
    }

    companion object {
        private val buttonShadow = DropShadow(4.0, 2.0, 2.0, Color.color(.0, 0.0, .0, .3))
    }
}
