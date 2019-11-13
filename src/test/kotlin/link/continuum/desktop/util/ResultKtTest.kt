package link.continuum.desktop.util

import koma.util.*
import kotlin.test.*

internal class ResultKtTest {

    @Test
    fun getOr() {
        val ex = Exception("ex")
        fun returnEx(): KResult<Unit, Exception> {
            val e: KResult<Unit, Exception> = Err(ex)
            val (t, err, r) = e
            if (r.testFailure(t, err)) {
                return Err(err)
            }
            return Ok(t)
        }

        val e = returnEx()
        assertFalse { e.isSuccess }
        assert(e.isFailure)
        assertNotSame(ex, Ok<Int, Exception>(5).getFailureOr { Exception() })
        assertNotSame(ex, Err<Int, Exception>(Exception("ex2")).getFailureOr { Exception() })
        assertSame(ex, e.getFailureOr { Exception() })
    }

    @Test
    fun nullableWith() {
        assertEquals(3, 1.given(2) { this.plus(it)})
        assertEquals(1, 1.given(null as Int?) { this.plus(it)})
        assertEquals(15, 4.given(5) { this.plus(it)}.given(6) { this.plus(it)})
        assertEquals(-1, 7.given(null as Int?) { this.plus(it)}.given(8){this.minus(it)})
    }
}

