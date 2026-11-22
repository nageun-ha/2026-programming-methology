package com.example.prm_project.judge

import com.example.prm_project.model.PoseFrame

/**
 * 위치 안내 기준값. 첫 통합 데모(#36) 후 조정한다.
 *
 * @property minBoxHeightRatio 관절 박스 높이가 화면 높이의 이 비율보다 작으면 [FramingHint.COME_CLOSER]
 * @property lowLightLikelihood 모든 관절 신뢰도가 이 값 미만이면 [FramingHint.LOW_LIGHT].
 *   #46 DetectionThresholds와 같은 값, #46 머지 후 하나로 합칠 것
 * @property stableFrames 같은 안내가 이 프레임 수만큼 이어져야 표시를 바꾼다
 * @property readyHoldMs 표시가 [FramingHint.OK]인 채로 이 시간이 지나면 준비 완료
 * @property edgeRatio 좌우 가장자리 비율. 가장자리 잘림 판단을 붙이는 다음 PR에서 쓴다
 */
data class FramingThresholds(
    val minBoxHeightRatio: Float = 0.5f,
    val lowLightLikelihood: Float = 0.5f,
    val stableFrames: Int = 5,
    val readyHoldMs: Long = 1500L,
    val edgeRatio: Float = 0.05f,
)

/**
 * [frame] 한 장으로 위치 안내를 정한다. 부수 효과가 없다.
 *
 * 관절 없음 → 신뢰도 전부 낮음 → 박스가 작음 순서로 보고, 모두 아니면 [FramingHint.OK]다.
 * 위치 안내는 감지 상태와 독립이므로 [PoseFrame.detection]은 보지 않는다.
 * 박스 높이는 정규화 y의 (최대 − 최소)라 이미 화면 높이 대비 비율이다.
 */
fun judgeFraming(frame: PoseFrame, thresholds: FramingThresholds = FramingThresholds()): FramingHint {
    val points = frame.landmarks.values
    if (points.isEmpty()) return FramingHint.NOT_VISIBLE
    if (points.all { it.likelihood < thresholds.lowLightLikelihood }) return FramingHint.LOW_LIGHT
    val boxHeight = points.maxOf { it.y } - points.minOf { it.y }
    if (boxHeight < thresholds.minBoxHeightRatio) return FramingHint.COME_CLOSER
    return FramingHint.OK
}
