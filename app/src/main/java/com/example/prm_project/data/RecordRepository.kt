package com.example.prm_project.data

import com.example.prm_project.model.ExerciseRecord
import com.example.prm_project.model.Grade
import java.time.LocalDate

class RecordRepository(private val dao: ExerciseRecordDao) {
    /** 운동 1회 기록을 추가한다. 등급별 횟수는 미수집이거나 판독 횟수 전체를 포함해야 한다. */
    suspend fun save(record: ExerciseRecord) {
        val gradeCounts = record.gradeCounts.toMap()
        if (gradeCounts.isNotEmpty()) {
            require(gradeCounts.keys.containsAll(Grade.entries)) { "Grade counts must include every grade." }
            require(gradeCounts.values.all { it >= 0 }) { "Grade counts must be non-negative." }
            require(gradeCounts.values.sumOf { it.toLong() } == record.judgedCount.toLong()) {
                "Grade counts must add up to judgedCount."
            }
        }
        dao.insert(
            ExerciseRecordEntity(
                epochDay = record.date.toEpochDay(),
                durationSec = record.durationSec,
                judgedCount = record.judgedCount,
                successCount = record.successCount,
                softwareFailCount = record.softwareFailCount,
                perfectCount = gradeCounts[Grade.PERFECT],
                greatCount = gradeCounts[Grade.GREAT],
                badCount = gradeCounts[Grade.BAD],
                missCount = gradeCounts[Grade.MISS],
            ),
        )
    }

    /** 시작일과 종료일을 포함하여 날짜순으로 조회한다. */
    suspend fun recordsBetween(from: LocalDate, to: LocalDate): List<ExerciseRecord> =
        dao.recordsBetween(from.toEpochDay(), to.toEpochDay()).map { row ->
            ExerciseRecord(
                date = LocalDate.ofEpochDay(row.epochDay),
                durationSec = row.durationSec,
                judgedCount = row.judgedCount,
                successCount = row.successCount,
                softwareFailCount = row.softwareFailCount,
                gradeCounts = buildMap {
                    row.perfectCount?.let { put(Grade.PERFECT, it) }
                    row.greatCount?.let { put(Grade.GREAT, it) }
                    row.badCount?.let { put(Grade.BAD, it) }
                    row.missCount?.let { put(Grade.MISS, it) }
                },
            )
        }
}
