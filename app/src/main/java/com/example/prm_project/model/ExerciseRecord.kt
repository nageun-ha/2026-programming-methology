package com.example.prm_project.model

import java.time.LocalDate

/**
 * 운동 한 번의 기록. 점수 = [successCount] / [judgedCount].
 *
 * @property durationSec 운동 시간(초)
 * @property judgedCount 판독 횟수
 * @property successCount 성공 횟수. 소프트웨어 원인 인식 실패도 포함한다.
 * @property softwareFailCount 소프트웨어 원인 인식 실패 횟수
 */
data class ExerciseRecord(
    val date: LocalDate,
    val durationSec: Int,
    val judgedCount: Int,
    val successCount: Int,
    val softwareFailCount: Int,
)
