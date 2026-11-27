package com.example.prm_project.data

import com.example.prm_project.model.ExerciseRecord
import java.time.LocalDate
import java.time.YearMonth

private val MONTHLY_BAR_VALUE: (ExerciseRecord) -> Long = { it.durationSec.toLong() }

/** 전체 성공 횟수를 전체 판독 횟수로 나눈다. 판독이 없으면 null이다. */
fun score(records: List<ExerciseRecord>): Double? {
    val judgedCount = records.sumOf { it.judgedCount.toLong() }
    if (judgedCount == 0L) return null
    return records.sumOf { it.successCount.toLong() }.toDouble() / judgedCount
}

/** [from]과 [to]를 포함해 완료 기록이 있는 날은 1, 나머지는 0으로 반환한다. */
fun dailyLevels(
    records: List<ExerciseRecord>,
    from: LocalDate,
    to: LocalDate,
): Map<LocalDate, Int> {
    val completedDays = records.mapTo(mutableSetOf()) { it.date }
    return (from.toEpochDay()..to.toEpochDay()).associate { epochDay ->
        val day = LocalDate.ofEpochDay(epochDay)
        day to if (day in completedDays) 1 else 0
    }
}

/** [month]의 1일부터 말일까지 날짜별 운동 시간을 초 단위로 반환한다. */
fun monthlyBars(records: List<ExerciseRecord>, month: YearMonth): List<Long> {
    val totals = LongArray(month.lengthOfMonth())
    for (record in records) {
        if (YearMonth.from(record.date) == month) {
            totals[record.date.dayOfMonth - 1] += MONTHLY_BAR_VALUE(record)
        }
    }
    return totals.toList()
}
