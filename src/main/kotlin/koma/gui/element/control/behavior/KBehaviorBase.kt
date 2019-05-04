package koma.gui.element.control.behavior

import javafx.scene.Node
import koma.gui.element.control.inputmap.KInputMap
import koma.gui.element.control.inputmap.MappingType
import koma.gui.element.control.inputmap.mapping.MouseMapping
import java.util.*

abstract class KBehaviorBase<N : Node>(val node: N) {
    private val installedDefaultMappings: MutableList<MappingType>
    private val childInputMapDisposalHandlers: MutableList<Runnable>

    abstract val inputMap: KInputMap<N>


    init {
        this.installedDefaultMappings = ArrayList()
        this.childInputMapDisposalHandlers = ArrayList()
    }

    open fun dispose() {
        // when we dispose a behavior, we do NOT want to dispose the InputMap,
        // as that can remove input mappings that were not installed by the
        // behavior. Instead, we want to only remove mappings that the behavior
        // itself installed. This can be done by removing all input mappings that
        // were installed via the 'addDefaultMapping' method.

        // remove default mappings only
        for (mapping in installedDefaultMappings) {
            inputMap.mappings.remove(mapping)
        }

        // Remove all default child mappings
        for (r in childInputMapDisposalHandlers) {
            r.run()
        }
    }

    protected fun addDefaultMapping(inputMap: KInputMap<N>, newMapping: List<MappingType>) {
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
    protected fun addDefaultMapping(vararg maps: MouseMapping) {
        addDefaultMapping(inputMap, maps.map { MappingType.Mouse(it) })

    }

    protected fun <T : Node> addDefaultChildMap(parentInputMap:KInputMap<T>, newChildInputMap: KInputMap<T>) {
        parentInputMap.childInputMaps.add(newChildInputMap)

        childInputMapDisposalHandlers.add(Runnable{ parentInputMap.childInputMaps.remove(newChildInputMap) })
    }

    protected fun createInputMap(): KInputMap<N> {
        // TODO re-enable when InputMap moves back to Node / Control
        return KInputMap(node)
    }

    protected fun removeMapping(key: Any) {
        val inputMap = inputMap
        inputMap.lookupMapping(key).ifPresent { mapping ->
            inputMap.mappings.remove(mapping)
            // FIXME installedDefaultMappings.remove(mapping)
        }
    }
}
