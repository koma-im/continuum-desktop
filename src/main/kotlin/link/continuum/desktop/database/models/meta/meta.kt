package link.continuum.desktop.database.models.meta

import io.requery.Column
import io.requery.Entity
import io.requery.Persistable
import io.requery.Table

@Table(name = "INFORMATION_SCHEMA.COLUMNS")
@Entity(model = "H2Info")
interface DbColumns: Persistable {
    @get:Column(length = Int.MAX_VALUE, name = "TABLE_SCHEMA")
    var tableSchema: String

    @get:Column(length = Int.MAX_VALUE, name = "TABLE_NAME")
    var tableName: String

    @get:Column(length = Int.MAX_VALUE, name = "COLUMN_NAME")
    var columnName: String
}