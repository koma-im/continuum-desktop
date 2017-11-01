package koma.matrix.event

data class GeneralEvent (
        val type: String,
        val content: Map<String, Any>
)

