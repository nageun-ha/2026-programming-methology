package com.example.prm_project.judge

import com.example.prm_project.model.DetectionState
import com.example.prm_project.model.Grade
import com.example.prm_project.model.Joint
import com.example.prm_project.model.JudgeResult
import com.example.prm_project.model.PoseFrame
import com.example.prm_project.model.TargetPose
import kotlin.math.abs
import kotlin.math.min

/**
 * 등급 기준값. 첫 통합 데모(#36) 뒤에 조정할 값이다.
 *
 * 일치율 1은 달성하기 어려우므로 [PERFECT_MIN]은 1보다 낮게 둔다.
 */
object GradeThresholds {
    const val PERFECT_MIN = 0.85f
    const val GREAT_MIN = 0.65f
    const val BAD_MIN = 0.35f

    /** 오차가 허용 오차의 이 배수 이상이면 관절 점수가 0이다. */
    const val ZERO_SCORE_TOLERANCE_MULTIPLIER = 3f
}

/**
 * 박자 하나를 판정한다.
 *
 * - [DetectionState.SOFTWARE_LOSS]: GREAT, recognitionFailed = true. 성공으로 센다.
 * - [DetectionState.USER_CAUSED_LOSS]: MISS.
 * - [DetectionState.DETECTED]: [angles]로 잰 각도의 일치율([matchRate])로 등급을 정한다.
 *
 * 일치율은 이 안에서만 쓰고 [JudgeResult]에 넣거나 저장하지 않는다.
 * 측정값이 없는 관절은 점수 0으로 치고 wrongJoints에 넣는다.
 */
fun judge(frame: PoseFrame, target: TargetPose, beatIndex: Int, elapsedMs: Long): JudgeResult {
    if (frame.detection == DetectionState.SOFTWARE_LOSS) {
        return JudgeResult(
            poseId = target.id,
            grade = Grade.GREAT,
            recognitionFailed = true,
            beatIndex = beatIndex,
            elapsedMs = elapsedMs,
        )
    }
    if (frame.detection == DetectionState.USER_CAUSED_LOSS) {
        return JudgeResult(
            poseId = target.id,
            grade = Grade.MISS,
            recognitionFailed = false,
            beatIndex = beatIndex,
            elapsedMs = elapsedMs,
        )
    }

    val measured = angles(frame)
    val rate = matchRate(measured, target)
    val grade = when {
        rate >= GradeThresholds.PERFECT_MIN -> Grade.PERFECT
        rate >= GradeThresholds.GREAT_MIN -> Grade.GREAT
        rate >= GradeThresholds.BAD_MIN -> Grade.BAD
        else -> Grade.MISS
    }
    val wrongJoints = target.targetAngles.filter { (joint, goal) ->
        val angle = measured[joint]
        angle == null || abs(angle - goal) > target.toleranceDeg
    }.keys.toList()

    return JudgeResult(
        poseId = target.id,
        grade = grade,
        recognitionFailed = false,
        beatIndex = beatIndex,
        elapsedMs = elapsedMs,
        wrongJoints = wrongJoints.ifEmpty { null },
    )
}

/**
 * [measured]가 [target]과 얼마나 맞는지 0~1로 돌려준다.
 *
 * 관절 점수 = 1 − min(오차 / (허용 오차 × [GradeThresholds.ZERO_SCORE_TOLERANCE_MULTIPLIER]), 1)이고,
 * 일치율은 관절 점수의 평균이다. 측정값이 없는 관절은 0점이고, 목표 관절이 없으면 0이다.
 */
internal fun matchRate(measured: Map<Joint, Float>, target: TargetPose): Float {
    if (target.targetAngles.isEmpty()) return 0f
    val zeroScoreError = target.toleranceDeg * GradeThresholds.ZERO_SCORE_TOLERANCE_MULTIPLIER
    val total = target.targetAngles.entries.sumOf { (joint, goal) ->
        val angle = measured[joint] ?: return@sumOf 0.0
        (1f - min(abs(angle - goal) / zeroScoreError, 1f)).toDouble()
    }
    return (total / target.targetAngles.size).toFloat()
}
