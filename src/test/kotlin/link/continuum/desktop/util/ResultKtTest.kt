package link.continuum.desktop.util

import com.github.kittinunf.result.Result
import kotlin.test.*

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

    @Test
    fun nullableGetOr() {
        assertEquals(1, null as Int? `?or` { 1 })
        assertEquals(null, null as Int? `?or?` { null })
        assertEquals(2, null as Int? `?or?` { null } `?or?` { 2 })
        assertEquals(3, null as Int? `?or?` { 3 } `?or?` { null })
        assertEquals(4, 4 as Int? `?or?` { 3 } `?or?` { null })
        assertEquals(6, 6 as Int? `?or?` { 3 } `?or` { 5 })
    }
}

