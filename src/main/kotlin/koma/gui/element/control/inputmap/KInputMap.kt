package koma.gui.element.control.inputmap

import javafx.beans.property.ReadOnlyObjectWrapper
import javafx.beans.property.SimpleObjectProperty
import javafx.collections.FXCollections
import javafx.collections.ListChangeListener
import javafx.collections.ObservableList
import javafx.event.Event
import javafx.event.EventHandler
import javafx.event.EventType
import javafx.scene.Node
import javafx.scene.input.KeyEvent
import javafx.scene.input.MouseEvent
import koma.gui.element.control.inputmap.mapping.KeyMapping
import koma.gui.element.control.inputmap.mapping.Mapping
import java.util.*
import java.util.function.Predicate
import kotlin.collections.HashMap

sealed class MappingType(val _mapping: Mapping<out Event>) {
    class Any private constructor(val mapping: Mapping<Event>): MappingType(mapping)
    class Key(val mapping: Mapping<KeyEvent>): MappingType(mapping)
    class Mouse(val mapping: Mapping<MouseEvent>): MappingType(mapping)

    val eventType: EventType<out Event>
        get() = when(this) {
            is Any -> this.mapping.eventType!!
            is Key -> this.mapping.eventType!!
            is Mouse -> this.mapping.eventType!!
        }

    val isDisabled
        get() = when(this) {
            is Any -> this.mapping.isDisabled
            is Key -> this.mapping.isDisabled
            is Mouse -> this.mapping.isDisabled
        }

    val mappingKey
        get() = this._mapping.mappingKey

    val isAutoConsume
        get() = this._mapping.isAutoConsume

    fun handleEvent(ev: Event) {
        if (ev is KeyEvent && this is Key) {
            this.mapping.eventHandler.handle(ev)
        } else if (ev is MouseEvent && this is Mouse) {
            this.mapping.eventHandler.handle(ev)
        } else if (this is Any){
            this.mapping.eventHandler.handle(ev)
        } else {
            System.err.println("Event $ev not handled in MappingType")
        }
    }

    fun testInterceptor(event: Event): Boolean? {
        if (event is KeyEvent && this is Key) {
            return this.mapping.testInterceptor(event)
        } else if (event is MouseEvent && this is Mouse) {
            return this.mapping.testInterceptor(event)
        }
        if (this is MappingType.Any) {
            return this.mapping.testInterceptor(event)
        }
        return null
    }

    fun getSpecificity(event: Event): Int {
        if (event is KeyEvent && this is Key) {
            return this.mapping.getSpecificity(event)
        } else if (event is MouseEvent && this is Mouse) {
            return this.mapping.getSpecificity(event)
        }
        if (this is MappingType.Any) {
            return this.mapping.getSpecificity(event)
        }
        return -1
    }
}

class KInputMap<N: Node>(private val node: Node): EventHandler<Event> {
    val mappings: ObservableList<MappingType> = FXCollections.observableArrayList()
    private val eventTypeMappings = HashMap<EventType<out Event>, MutableList<MappingType>>()
    private val installedEventHandlers = HashMap<EventType<*>, MutableList<EventHandler<in Event>>>()
    val childInputMaps: ObservableList<KInputMap<N>> = FXCollections.observableArrayList<KInputMap<N>>()

    fun addKeyMappings(ms: List<KeyMapping>) {
        val m = ms.map { MappingType.Key(it) }
        this.mappings.addAll(m)
    }
    private val _parentInputMap = object :ReadOnlyObjectWrapper<KInputMap<N>>(this, "parentInputMap") {
        override fun invalidated() {
            // whenever the parent InputMap changes, we uninstall all mappings and
            // then reprocess them so that they are installed in the correct root.
            reprocessAllMappings()
        }
    }
    private var parentInputMap
        get() = _parentInputMap.get()
        set(value) = _parentInputMap.set(value)

    private val interceptorProperty = SimpleObjectProperty<Predicate<Event>>(this, "interceptor")
    var interceptor: Predicate<Event>?
        get() = interceptorProperty.get()
        set(value) = interceptorProperty.set(value)

    init {
        mappings.addListener(ListChangeListener<MappingType> { c -> processMappingsChange(c) })
        childInputMaps.addListener(object: ListChangeListener<KInputMap<N>> {
            override fun onChanged(c: ListChangeListener.Change<out KInputMap<N>>) {
                processChildMapsChange(c)
            }
        })
    }

    private fun processChildMapsChange(c: ListChangeListener.Change<out KInputMap<N>>) {
        while (c.next()) {
            if (c.wasRemoved()) {
                for (map in c.removed) {
                    map.parentInputMap = null
                }
            }

            if (c.wasAdded()) {
                val toRemove = mutableListOf<KInputMap<N>>()
                for (map in c.addedSubList) {
                    // we check that the child input map maps to the same node
                    // as this input map
                    if (map.node !== node) {
                        toRemove.add(map)
                    } else {
                        map.parentInputMap = this
                    }
                }

                if (!toRemove.isEmpty()) {
                    childInputMaps.removeAll(toRemove)
                    throw IllegalArgumentException("Child InputMap intances need to share a common Node object")
                }
            }
        }
    }

    private fun processMappingsChange(c: ListChangeListener.Change<out MappingType>) {
        while (c.next()) {
        // TODO handle mapping removal
        if (c.wasRemoved()) {
            for (mapping in c.getRemoved()) {
                removeMapping(mapping)
            }
        }

        if (c.wasAdded()) {
            val toRemove = mutableListOf<MappingType?>()
            for (mapping in c.getAddedSubList()) {
                if (mapping == null) {
                    toRemove.add(null)
                } else {
                    addMapping(mapping)
                }
            }

            if (!toRemove.isEmpty()) {
                mappings.removeAll(toRemove)
                throw IllegalArgumentException("Null mappings not permitted")
            }
        }
    }}

    /**
     * Invoked when a specific event of the type for which this handler is
     * registered happens.
     *
     * @param event the event which occurred
     */
    override fun handle(e: Event?) {
        if (e == null || e.isConsumed) return

        val mappings = lookup(e, true)
        for (mapping in mappings) {
            mapping.handleEvent(e)

            if (mapping.isAutoConsume) {
                e.consume()
            }

            if (e.isConsumed()) {
                break
            }

            // If we are here, the event has not been consumed, so we continue
            // looping through our list of matches. Refer to the documentation in
            // lookup(Event) for more details on the list ordering.
        }
    }

    private fun removeMapping(mapping: MappingType) {
        val et: EventType<out Event> = mapping.eventType
        this.eventTypeMappings.get(et)?.remove(mapping)
    }

    private fun addMapping(mapping: MappingType) {
        val rootInputMap = getRootInputMap();
        rootInputMap.addEventHandler(mapping.eventType)

        // we maintain a separate map of all mappings, which maps from the
        // mapping event type into a list of mappings. This allows for easier
        // iteration in the lookup methods.
        val et: EventType<out Event>? = mapping.eventType
        val ms = this.eventTypeMappings.computeIfAbsent(et!!) { mutableListOf() }
        ms.add(mapping)
    }

    private fun getRootInputMap(): KInputMap<N> {
        var rootInputMap: KInputMap<N> = this
        while (true) {
            val parentInputMap = rootInputMap.parentInputMap ?: break
            rootInputMap = parentInputMap
        }
        return rootInputMap
    }

    private fun addEventHandler(et: EventType<out Event>) {
        val eventHandlers = installedEventHandlers.computeIfAbsent(et) { _ -> mutableListOf() }

        val eventHandler = EventHandler<Event> { this.handle(it) }

        if (eventHandlers.isEmpty()) {
            //println("Added event handler for type $et")
            node.addEventHandler(et, eventHandler)
        }

        // We need to store these event handlers so we can dispose cleanly.
        eventHandlers.add(eventHandler)
    }

    private fun removeAllEventHandlers() {
        for ((et, handlers) in installedEventHandlers.entries) {
            for (handler in handlers)  {
                //println("Removed event handler for type $et");
                node.removeEventHandler(et, handler)
            }
        }
    }

    private fun reprocessAllMappings() {
        removeAllEventHandlers()
        this.mappings.forEach { this.addMapping(it) }

        // now do the same for all children
        for (child in childInputMaps) {
            child.reprocessAllMappings()
        }
    }

    fun dispose() {
        for (childInputMap in childInputMaps) {
            childInputMap.dispose()
        }

        // uninstall event handlers
        removeAllEventHandlers()

        // clear out all mappings
        mappings.clear()
    }

    private fun lookupMappingAndSpecificity(
            event: Event, minSpecificity: Int
    ): List<Pair<Int, MappingType>> {
        var _minSpecificity = minSpecificity

        val mappings = this.eventTypeMappings.getOrDefault(event.eventType, mutableListOf())
        val result = mutableListOf<Pair<Int, MappingType>>()
        for (mapping in mappings) {
            if (mapping.isDisabled) continue

            // test if mapping has an interceptor that will block this event.
            // Interceptors return true if the interception should occur.
            val interceptorsApplies = mapping.testInterceptor(event)
            if (interceptorsApplies == true) {
                continue
            }

            val specificity = mapping.getSpecificity(event)
            if (specificity > 0 && specificity == _minSpecificity) {
                result.add(Pair(specificity, mapping))
            } else if (specificity > _minSpecificity) {
                result.clear()
                result.add(Pair(specificity, mapping))
                _minSpecificity = specificity
            }
        }

        return result
    }

    private fun lookup(event: Event, testInterceptors: Boolean): List<MappingType> {
        // firstly we look at ourselves to see if we have a mapping, assuming our
        // interceptors are valid
        if (testInterceptors) {
            val interceptorsApplies = interceptor?.test(event)

            if (interceptorsApplies == true) {
                return listOf()
            }
        }

        val mappings = mutableListOf<MappingType>()

        var minSpecificity = 0
        val results = lookupMappingAndSpecificity(event, minSpecificity)
        if (!results.isEmpty()) {
            minSpecificity = results[0].first
            mappings.addAll( results.map { it.second } )
        }

        // but we always descend into our child input maps as well, to see if there
        // is a more specific mapping there. If there is a mapping of equal
        // specificity, we take the child mapping over the parent mapping.
        for (childInputMap in childInputMaps) {
            minSpecificity = scanRecursively(childInputMap, event, testInterceptors, minSpecificity,
                    mappings)
        }

        return mappings
    }

    private fun scanRecursively(inputMap: KInputMap<*>, event: Event, testInterceptors: Boolean, minSpecificity: Int,
                                mappings: MutableList<MappingType>): Int {
        var minSpecificity1 = minSpecificity
        // test if the childInputMap should be considered
        if (testInterceptors) {
            val interceptorsApplies = inputMap.interceptor?.test(event)
            if (interceptorsApplies == true) {
                return minSpecificity1
            }
        }

        // look at the given InputMap
        val childResults = inputMap.lookupMappingAndSpecificity(event, minSpecificity1)
        if (!childResults.isEmpty()) {
            val specificity = childResults[0].first
            val childMappings = childResults.map { pair -> pair.second }
            if (specificity == minSpecificity1) {
                mappings.addAll(0, childMappings)
            } else if (specificity > minSpecificity1) {
                mappings.clear()
                minSpecificity1 = specificity
                mappings.addAll(childMappings)
            }
        }

        // now look at the children of this input map, if any exist
        for (i in 0 until inputMap.childInputMaps.size) {
            minSpecificity1 = scanRecursively(inputMap.childInputMaps[i], event, testInterceptors, minSpecificity1, mappings)
        }

        return minSpecificity1
    }

    private fun lookupMappingKey(mappingKey: Any): List<MappingType> {
        return mappings
                .filter { !it.isDisabled }
                .filter { it.mappingKey == mappingKey }
    }

    fun lookupMapping(mappingKey: Any?): Optional<MappingType> {
        if (mappingKey == null) {
            return Optional.empty()
        }

        val mappings = lookupMappingKey(mappingKey).toMutableList()

        // descend into our child input maps as well
        for (childInputMap in childInputMaps) {
            val childMappings = childInputMap.lookupMappingKey(mappingKey)
            mappings.addAll(0, childMappings)
        }
        return if (mappings.size > 0) Optional.of(mappings[0]) else Optional.empty()
    }
}
