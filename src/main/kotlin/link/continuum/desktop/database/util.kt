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
import link.continuum.desktop.database.models.DbColumns
import mu.KotlinLogging
import org.h2.jdbcx.JdbcConnectionPool
import org.h2.jdbcx.JdbcDataSource
import kotlin.time.ExperimentalTime
import kotlin.time.measureTimedValue
import link.continuum.desktop.database.models.Models as DbModels

typealias KDataStore = KotlinEntityDataStore<Persistable>

private val logger = KotlinLogging.logger {}

@ExperimentalTime
fun openStore(dbPath: String, level: Int=1): KotlinEntityDataStore<Persistable> {
    val (s, t) = measureTimedValue {
        val ds = JdbcDataSource()
        ds.setURL("jdbc:h2:$dbPath;TRACE_LEVEL_FILE=0;TRACE_LEVEL_SYSTEM_OUT=$level")
        val dsp = JdbcConnectionPool.create(ds)


        val conf = KotlinConfiguration(dataSource = dsp, model = Models.DEFAULT)
        val s = KotlinEntityDataStore<Persistable>(conf)
        val sm = SchemaModifier(conf)
        sm.createTables(TableCreationMode.CREATE_NOT_EXISTS)
        addMissingColumn(dsp, sm)
        s
    }
    logger.debug { "opening db takes $t"}
    return s
}

private fun addMissingColumn(
        dataSource: JdbcConnectionPool,
        modifier: SchemaModifier
) {
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