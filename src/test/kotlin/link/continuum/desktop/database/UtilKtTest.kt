package link.continuum.desktop.database

import link.continuum.database.openStore
import mu.KotlinLogging
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.nio.file.Files
import java.nio.file.Path
import kotlin.time.ExperimentalTime

private val logger = KotlinLogging.logger {}

@ExperimentalTime
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class UtilKtTest {

    @Test
    fun testOpenStore() {
        openStore(dir.toString())
    }
    companion object {
        val tmp = Files.createTempDirectory("continuum-test")
        val dir: Path = tmp.resolve("test")
        @AfterAll
        @JvmStatic
        fun teardown() {
            tmp.toFile().deleteRecursively()
        }
    }
}