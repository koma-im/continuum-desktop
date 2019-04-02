package link.continuum.desktop.database

import io.requery.Persistable
import io.requery.sql.KotlinConfiguration
import io.requery.sql.KotlinEntityDataStore
import io.requery.sql.SchemaModifier
import io.requery.sql.TableCreationMode
import link.continuum.desktop.database.models.Models
import org.h2.jdbcx.JdbcConnectionPool
import org.h2.jdbcx.JdbcDataSource

typealias KDataStore = KotlinEntityDataStore<Persistable>

internal fun openStore(dbPath: String): KotlinEntityDataStore<Persistable> {
    val ds = JdbcDataSource()
    ds.setURL("jdbc:h2:$dbPath;TRACE_LEVEL_FILE=0;TRACE_LEVEL_SYSTEM_OUT=1")
    val dsp = JdbcConnectionPool.create(ds)
    val conf = KotlinConfiguration(dataSource = dsp, model = Models.DEFAULT)
    val s = KotlinEntityDataStore<Persistable>(conf)
    val sm = SchemaModifier(conf)
    sm.createTables(TableCreationMode.CREATE_NOT_EXISTS)
    return s
}
