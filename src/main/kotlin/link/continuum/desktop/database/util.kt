package link.continuum.database

import io.requery.Persistable
import io.requery.kotlin.eq
import io.requery.kotlin.findAttribute
import io.requery.sql.KotlinConfiguration
import io.requery.sql.KotlinEntityDataStore
import io.requery.sql.SchemaModifier
import io.requery.sql.TableCreationMode
import link.continuum.database.models.Membership
import link.continuum.database.models.Models
import link.continuum.desktop.database.models.meta.DbColumns
import mu.KotlinLogging
import org.h2.jdbcx.JdbcConnectionPool
import org.h2.jdbcx.JdbcDataSource
import kotlin.time.ExperimentalTime
import link.continuum.desktop.database.models.meta.Models as DbModels
import kotlin.time.MonoClock

typealias KDataStore = KotlinEntityDataStore<Persistable>

private val logger = KotlinLogging.logger {}

@ExperimentalTime
fun openStore(dbPath: String, level: Int=1): KotlinEntityDataStore<Persistable> {
    val mark = MonoClock.markNow()
    val dataSource = JdbcDataSource()
    dataSource.setURL("jdbc:h2:$dbPath;TRACE_LEVEL_FILE=0;TRACE_LEVEL_SYSTEM_OUT=$level")
    val dataSourcePool = JdbcConnectionPool.create(dataSource)
    val conf = KotlinConfiguration(dataSource = dataSourcePool, model = Models.DEFAULT)
    val s = KotlinEntityDataStore<Persistable>(conf)
    logger.trace { "opening db takes ${mark.elapsedNow()}"}
    val columns = getDbColumns(dataSourcePool)
    logger.trace { "opening db and getting columns takes ${mark.elapsedNow()}"}
    val sm = SchemaModifier(conf)
    val addedTypes = Models.DEFAULT.types.filterNot {
        columns.contains(it.name.toLowerCase())
    }
    if (addedTypes.isNotEmpty()) {
        logger.info { "creating tables of $addedTypes"}
        sm.createTables(TableCreationMode.CREATE_NOT_EXISTS)
    }
    addMissingColumn(columns, sm)
    return s
}

private fun getDbColumns(dataSource: JdbcConnectionPool): Map<String, HashSet<String>> {
    val s0 = KotlinEntityDataStore<Persistable>(
            KotlinConfiguration(dataSource = dataSource, model = DbModels.H2INFO)
    )
    val existingColumns = hashMapOf<String, HashSet<String>>()
    s0.select(DbColumns::class)
            .where(DbColumns::tableSchema.eq("PUBLIC"))
            .get().forEach {
                existingColumns.computeIfAbsent(it.tableName.toLowerCase()) {
                    hashSetOf()
                }.add(it.columnName.toLowerCase())
            }
    return existingColumns
}

private fun addMissingColumn(
        existingColumns: Map<String, HashSet<String>>,
        modifier: SchemaModifier
) {
    arrayOf(Membership::since,
            Membership::joiningRoom
    ).map {
        findAttribute(it)
    }.filter {
        val t = it.declaringType.name
        val typeColumns = existingColumns[t.toLowerCase()]
        check(typeColumns != null) { "Table $t unknown" }
        val c = it.name
        if (!typeColumns.contains(c.toLowerCase())) {
            logger.debug { "column $c of $t has not been added"}
            true
        } else {
            false
        }
    }.forEach { attribute ->
        logger.debug { "adding column '${attribute.name}' to table ${attribute.declaringType.name}"}
        modifier.addColumn(attribute)
    }
}