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

    /**
     * 화면 밖으로 잘린 목표 각도가 있을 때, 판정하려면 목표 각도 중 이 비율 이상(그리고 1개 이상)을 재야 한다.
     * 못 미치면 사용자 원인으로 보고 MISS다. 첫 통합 데모(#36) 후 조정한다.
     */
    const val MIN_MEASURED_RATIO = 0.5f
}

/**
 * 박자 하나를 판정한다.
 *
 * 목표 각도는 각도에 쓰는 세 관절([JOINT_ANGLES]의 꼭짓점과 양 끝) 기준으로 셋 중 하나다.
 * - 측정됨: [angles]에 값이 있다.
 * - 잘림: 값이 없고, 세 관절 중 하나라도 [PoseFrame.outOfFrame]에 있다.
 * - 가려짐: 값이 없고 잘림도 아니다(저신뢰 등).
 *
 * 1. [DetectionState.SOFTWARE_LOSS]: GREAT, recognitionFailed = true. 성공으로 센다.
 * 2. [DetectionState.USER_CAUSED_LOSS]이고 관절이 하나도 없음: MISS.
 * 3. 잘린 각도가 있음: 측정된 각도가 1개 이상이고 목표 각도 중 [GradeThresholds.MIN_MEASURED_RATIO] 이상이면
 *    측정된 각도만으로 일치율([matchRate])을 내고, 아니면 MISS다. 감지 상태(USER_CAUSED_LOSS 포함)와 상관없다.
 * 4. 잘린 각도가 없음: 가려진 각도는 빼고 일치율로 등급을 정한다. 하나도 잴 수 없으면
 *    [DetectionState.DETECTED]는 1번과 같고(#3: 화면 안인데 전부 저신뢰), [DetectionState.USER_CAUSED_LOSS]는 MISS다
 *    (다른 관절이 화면 밖이라 사용자 원인).
 *
 * wrongJoints에는 잴 수 있었고 오차가 허용 오차를 넘은 관절만 넣는다. 잘리거나 가려진 각도는 넣지 않고, PERFECT면 null이다.
 * 일치율은 이 안에서만 쓰고 [JudgeResult]에 넣거나 저장하지 않는다.
 */
fun judge(frame: PoseFrame, target: TargetPose, beatIndex: Int, elapsedMs: Long): JudgeResult {
    if (frame.detection == DetectionState.SOFTWARE_LOSS) return recognitionFailed(target, beatIndex, elapsedMs)
    if (frame.detection == DetectionState.USER_CAUSED_LOSS && frame.landmarks.isEmpty()) {
        return userCausedMiss(target, beatIndex, elapsedMs)
    }

    val measured = angles(frame)
    val measuredCount = target.targetAngles.keys.count { it in measured }
    val clipped = target.targetAngles.keys.any { it !in measured && isClipped(it, frame) }
    if (clipped) {
        val enough = measuredCount >= 1 &&
            measuredCount >= GradeThresholds.MIN_MEASURED_RATIO * target.targetAngles.size
        if (!enough) return userCausedMiss(target, beatIndex, elapsedMs)
    } else if (measuredCount == 0) {
        return if (frame.detection == DetectionState.USER_CAUSED_LOSS) {
            userCausedMiss(target, beatIndex, elapsedMs)
        } else {
            recognitionFailed(target, beatIndex, elapsedMs)
        }
    }

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

private fun userCausedMiss(target: TargetPose, beatIndex: Int, elapsedMs: Long) = JudgeResult(
    poseId = target.id,
    grade = Grade.MISS,
    recognitionFailed = false,
    beatIndex = beatIndex,
    elapsedMs = elapsedMs,
)

/** [vertex] 각도에 쓰는 세 관절 중 하나라도 화면 밖이면 true. [JOINT_ANGLES]에 없는 각도는 잴 수 없으므로 잘림이 아니다. */
private fun isClipped(vertex: Joint, frame: PoseFrame): Boolean {
    val (a, c) = JOINT_ANGLES[vertex] ?: return false
    return a in frame.outOfFrame || vertex in frame.outOfFrame || c in frame.outOfFrame
}

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
