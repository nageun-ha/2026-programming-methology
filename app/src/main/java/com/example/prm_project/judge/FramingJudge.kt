package com.example.prm_project.judge

import com.example.prm_project.model.DetectionState
import com.example.prm_project.model.PoseFrame

/**
 * 위치 안내 기준값. 첫 통합 데모(#36) 후 조정한다.
 *
 * @property minBoxHeightRatio 관절 박스 높이가 화면 높이의 이 비율보다 작으면 [FramingHint.COME_CLOSER]
 * @property stableFrames 같은 안내가 이 프레임 수만큼 이어져야 표시를 바꾼다
 * @property readyHoldMs 표시가 [FramingHint.OK]인 채로 이 시간이 지나면 준비 완료
 * @property edgeWarningLevel [PoseFrame.edge]의 한 방향 값이 이 이상이면 그 방향이 잘린 것으로 본다.
 *   edge는 가장자리 안쪽 5%에서 0, 경계에서 0.5, 경계 밖 5%에서 1이라 0.5는 관절이 경계에 닿은 지점이다.
 *   0보다 크기만 해도 안내하면 팔을 옆으로 뻗는 자세에서 가장자리 근처라는 이유만으로 안내가 나온다.
 */
data class FramingThresholds(
    val minBoxHeightRatio: Float = 0.5f,
    val stableFrames: Int = 5,
    val readyHoldMs: Long = 1500L,
    val edgeWarningLevel: Float = 0.5f,
)

/**
 * [frame] 한 장으로 위치 안내를 정한다. 부수 효과가 없다.
 *
 * 1. [DetectionState.SOFTWARE_LOSS] → [PoseFrame.lowConfidence]가 있으면 [FramingHint.LOW_LIGHT], 없으면
 *    [FramingHint.NOT_VISIBLE]. 직전 관절이 그대로 들어 있어 옛 좌표로 OK가 나와 준비 시간이 쌓이지 않게 먼저 본다.
 *    이때 edge는 항상 0이다.
 * 2. 가장자리([PoseFrame.edge], 화면 기준): 위·아래 또는 좌우 양쪽 → [FramingHint.STEP_BACK],
 *    왼쪽만 → [FramingHint.MOVE_RIGHT], 오른쪽만 → [FramingHint.MOVE_LEFT]. 잘린 쪽의 반대로 가라는 안내다.
 *    잡힌 관절이 없어도 본다. 화면 밖으로 나가 신뢰도 높은 관절이 없을 때 "어두워요" 대신 돌아올 방향을 알려 주기 위해서다
 *    (완전히 나가면 edge는 직전 값을 유지한다, #50).
 * 3. 잡힌 관절이 없음 → 1번과 같이 [FramingHint.LOW_LIGHT] 또는 [FramingHint.NOT_VISIBLE].
 * 4. 박스가 작음 → [FramingHint.COME_CLOSER]. 박스 높이는 정규화 y의 (최대 − 최소)라 이미 화면 높이 대비 비율이다.
 * 5. 모두 아니면 [FramingHint.OK].
 */
fun judgeFraming(frame: PoseFrame, thresholds: FramingThresholds = FramingThresholds()): FramingHint {
    val notFound = if (frame.lowConfidence.isEmpty()) FramingHint.NOT_VISIBLE else FramingHint.LOW_LIGHT
    if (frame.detection == DetectionState.SOFTWARE_LOSS) return notFound

    val level = thresholds.edgeWarningLevel
    val left = frame.edge.left >= level
    val right = frame.edge.right >= level
    when {
        frame.edge.top >= level || frame.edge.bottom >= level || (left && right) -> return FramingHint.STEP_BACK
        left -> return FramingHint.MOVE_RIGHT
        right -> return FramingHint.MOVE_LEFT
    }

    val points = frame.landmarks.values
    if (points.isEmpty()) return notFound
    val boxHeight = points.maxOf { it.y } - points.minOf { it.y }
    if (boxHeight < thresholds.minBoxHeightRatio) return FramingHint.COME_CLOSER
    return FramingHint.OK
}
