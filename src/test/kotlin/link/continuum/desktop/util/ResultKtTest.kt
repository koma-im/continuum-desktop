package link.continuum.desktop.util

import com.github.kittinunf.result.Result
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotSame
import kotlin.test.assertSame

internal class ResultKtTest {

    @Test
    fun getOr() {
        val ex = Exception("ex")
        fun returnEx(): KResult<Unit, Exception> {
            val e: KResult<Unit, Exception> = Err(ex)
            val t = e getOr { return it }
            return Ok(t)
        }

        val e = returnEx()
        assertFalse { e.isOk() }
        assert(e is Result.Failure)
        assertNotSame(ex, Ok<Int, Exception>(5).getErrOr { Exception() })
        assertNotSame(ex, Err<Int, Exception>(Exception("ex2")).getErrOr { Exception() })
        assertSame(ex, e.getErrOr { Exception() })
    }
}

