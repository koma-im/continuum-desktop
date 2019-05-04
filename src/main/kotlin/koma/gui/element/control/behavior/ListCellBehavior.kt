/*
 * Copyright (c) 2010, 2015, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 */

package koma.gui.element.control.behavior

import javafx.scene.control.FocusModel
import javafx.scene.control.ListCell
import javafx.scene.control.ListView
import javafx.scene.control.MultipleSelectionModel

class ListCellBehavior<T>(control: ListCell<T>) : CellBehaviorBase<ListCell<T>>(control) {

    override val selectionModel: MultipleSelectionModel<T>?
        get() = cellContainer.selectionModel

    override val focusModel: FocusModel<T>?
        get() = cellContainer.focusModel

    override val cellContainer: ListView<T>
        get() = node.listView

    override fun edit(cell: ListCell<T>?) {
        val index = cell?.index ?: -1
        cellContainer.edit(index)
    }

}
