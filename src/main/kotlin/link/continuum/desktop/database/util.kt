package link.continuum.database

import io.requery.Persistable
import io.requery.sql.KotlinConfiguration
import io.requery.sql.KotlinEntityDataStore
import io.requery.sql.SchemaModifier
import io.requery.sql.TableCreationMode
import link.continuum.database.models.Models
import mu.KotlinLogging
import org.h2.jdbcx.JdbcConnectionPool
import org.h2.jdbcx.JdbcDataSource
import kotlin.time.ExperimentalTime
import kotlin.time.measureTimedValue

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
        s
    }
    logger.debug { "opening db takes $t"}
    return s
}