package link.continuum.desktop.util

import koma.util.formatJson
import kotlin.test.Test

internal class JsonFormatKtTest {

    @Test
    fun getOr() {
        val s = "{\"1\":2}"
        formatJson(s)
    }
}
