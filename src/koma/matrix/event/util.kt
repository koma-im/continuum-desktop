package koma.matrix.event

import koma.matrix.json.NewTypeString

data class EventId(private val string: String): NewTypeString(string)
