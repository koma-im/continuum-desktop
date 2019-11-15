package link.continuum.desktop.util

@Suppress("NON_PUBLIC_PRIMARY_CONSTRUCTOR_OF_INLINE_CLASS")
inline class UShortPair private constructor(private val data: Int) {
    constructor(first: UShort, second: UShort
    ) : this(joinBits(first, second))

    val first: UShort
        get() = data.ushr(16).toUShort()

    val second: UShort
        get() = data.and(0x0000FFFF).toUShort()

    override fun toString (): String {
        return "($first, $second)"
    }

    companion object {
        private fun joinBits(first: UShort, second: UShort): Int {
            return first.toInt().shl(16).or(second.toInt())
        }
    }
}