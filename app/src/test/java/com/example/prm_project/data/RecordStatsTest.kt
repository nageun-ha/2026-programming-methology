package com.example.prm_project.data

import com.example.prm_project.model.ExerciseRecord
import java.time.LocalDate
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RecordStatsTest {
    private val day = LocalDate.of(2024, 2, 29)

    @Test
    fun `no judgments means no score`() {
        assertNull(score(emptyList()))
        assertNull(score(listOf(record(day, 20, 0, 0))))
    }

    @Test
    fun `two sessions on the same day are combined`() {
        val records = listOf(record(day, 20, 2, 1), record(day, 40, 6, 5))

        assertEquals(0.75, requireNotNull(score(records)), 0.0)
        val previousDay = day.minusDays(1)
        assertEquals(mapOf(previousDay to 0, day to 1), dailyLevels(records, previousDay, day))
        assertEquals(60L, monthlyBars(records, YearMonth.from(day)).last())
    }

    @Test
    fun `completed sessions earn grass regardless of judgment outcome`() {
        val nextDay = day.plusDays(1)
        val records = listOf(
            record(day, 20, 3, 0),
            ExerciseRecord(nextDay, 30, 3, 3, softwareFailCount = 3),
        )
        assertEquals(mapOf(day to 1, nextDay to 1), dailyLevels(records, day, nextDay))
    }

    @Test
    fun `monthly bars include empty days and exclude other months and years`() {
        val records = listOf(
            record(day, 20, 3, 0),
            record(day.plusDays(1), 40, 1, 1),
            record(day.minusYears(1), 60, 1, 1),
        )
        assertEquals(List(28) { 0L } + 20L, monthlyBars(records, YearMonth.from(day)))
    }

    private fun record(date: LocalDate, seconds: Int, judged: Int, success: Int) =
        ExerciseRecord(date, seconds, judged, success, softwareFailCount = 0)
}
