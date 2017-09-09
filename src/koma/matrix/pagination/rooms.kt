package koma.matrix.pagination

data class RoomBatch<T>(
        val total_room_count_estimate: Int,
        // probaby can be null when the transferring is done
        val next_batch: String?,
        val chunk: List<T>
)

