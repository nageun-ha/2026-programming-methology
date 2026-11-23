package com.example.prm_project.data

import com.example.prm_project.model.ExerciseRecord
import com.example.prm_project.model.Grade
import java.time.LocalDate

class RecordRepository(private val dao: ExerciseRecordDao) {
    /** 운동 1회 기록을 추가한다. 같은 날짜의 기존 기록도 유지한다. */
    suspend fun save(record: ExerciseRecord) {
        dao.insert(
            ExerciseRecordEntity(
                epochDay = record.date.toEpochDay(),
                durationSec = record.durationSec,
                judgedCount = record.judgedCount,
                successCount = record.successCount,
                softwareFailCount = record.softwareFailCount,
                perfectCount = record.gradeCounts[Grade.PERFECT],
                greatCount = record.gradeCounts[Grade.GREAT],
                badCount = record.gradeCounts[Grade.BAD],
                missCount = record.gradeCounts[Grade.MISS],
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
