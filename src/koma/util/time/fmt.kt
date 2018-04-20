package koma.util.time

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

fun utcDateFrom(inst: Long): LocalDate {
    return Instant.ofEpochMilli(inst).atOffset(ZoneOffset.UTC).toLocalDate()
}
