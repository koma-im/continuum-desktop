package koma.matrix.room.visibility

enum class HistoryVisibility {
    Invited,
    Joined,
    Shared,
    WorldReadable;

    companion object {
        fun fromString(hvstr: String): HistoryVisibility? {
            val vis = when (hvstr) {
                "invited" -> HistoryVisibility.Invited
                "joined" -> HistoryVisibility.Joined
                "shared" -> HistoryVisibility.Shared
                "world_readable" -> HistoryVisibility.WorldReadable
                else -> null
            }
            return vis
        }
    }
}
