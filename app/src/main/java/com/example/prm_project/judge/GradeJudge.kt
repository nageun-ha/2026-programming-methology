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
    /** 오차가 허용 오차의 이 배수 이상이면 관절 점수가 0이다. */
    const val ZERO_SCORE_TOLERANCE_MULTIPLIER = 3f

    const val PERFECT_MIN = 0.85f

    /** 허용 오차 경계에 있는 관절의 점수. 관절이 하나뿐이면 "허용 오차 안 = GREAT 이상"이 된다. */
    const val GREAT_MIN = 1f - 1f / ZERO_SCORE_TOLERANCE_MULTIPLIER
    const val BAD_MIN = 0.35f
}

/**
 * 박자 하나를 판정한다.
 *
 * - [DetectionState.SOFTWARE_LOSS]: GREAT, recognitionFailed = true. 성공으로 센다.
 * - [DetectionState.USER_CAUSED_LOSS]: MISS.
 * - [DetectionState.DETECTED]: [angles]로 잰 각도의 일치율([matchRate])로 등급을 정한다.
 *   - 측정값이 없는 관절(가려져 신뢰도가 낮은 관절)은 빼고 남은 관절로 판정한다.
 *   - 목표 관절을 하나도 잴 수 없으면 [DetectionState.SOFTWARE_LOSS]와 같게 처리한다 (#3: 화면 안인데 전부 저신뢰).
 *
 * wrongJoints에는 잴 수 있었고 오차가 허용 오차를 넘은 관절만 넣는다. PERFECT면 null이다.
 * 일치율은 이 안에서만 쓰고 [JudgeResult]에 넣거나 저장하지 않는다.
 */
fun judge(frame: PoseFrame, target: TargetPose, beatIndex: Int, elapsedMs: Long): JudgeResult {
    if (frame.detection == DetectionState.SOFTWARE_LOSS) return recognitionFailed(target, beatIndex, elapsedMs)
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
    if (target.targetAngles.keys.none { it in measured }) return recognitionFailed(target, beatIndex, elapsedMs)

    val rate = matchRate(measured, target)
    val grade = when {
        rate >= GradeThresholds.PERFECT_MIN -> Grade.PERFECT
        rate >= GradeThresholds.GREAT_MIN -> Grade.GREAT
        rate >= GradeThresholds.BAD_MIN -> Grade.BAD
        else -> Grade.MISS
    }
    val wrongJoints = if (grade == Grade.PERFECT) emptyList() else target.targetAngles.filter { (joint, goal) ->
        val angle = measured[joint] ?: return@filter false
        abs(angle - goal) > target.toleranceDeg
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

private fun recognitionFailed(target: TargetPose, beatIndex: Int, elapsedMs: Long) = JudgeResult(
    poseId = target.id,
    grade = Grade.GREAT,
    recognitionFailed = true,
    beatIndex = beatIndex,
    elapsedMs = elapsedMs,
)

/**
 * [measured]가 [target]과 얼마나 맞는지 0~1로 돌려준다.
 *
 * 관절 점수 = 1 − min(오차 / (허용 오차 × [GradeThresholds.ZERO_SCORE_TOLERANCE_MULTIPLIER]), 1)이고,
 * 일치율은 측정값이 있는 관절 점수의 평균이다. 잴 수 있는 목표 관절이 없으면 0이다.
 */
internal fun matchRate(measured: Map<Joint, Float>, target: TargetPose): Float {
    val zeroScoreError = target.toleranceDeg * GradeThresholds.ZERO_SCORE_TOLERANCE_MULTIPLIER
    val scores = target.targetAngles.mapNotNull { (joint, goal) ->
        val angle = measured[joint] ?: return@mapNotNull null
        1f - min(abs(angle - goal) / zeroScoreError, 1f)
    }
    return if (scores.isEmpty()) 0f else scores.average().toFloat()
}
