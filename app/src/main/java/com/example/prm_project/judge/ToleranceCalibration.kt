package com.example.prm_project.judge

import com.example.prm_project.model.Joint
import com.example.prm_project.model.TargetPose
import kotlin.math.abs

/**
 * 허용 오차 보정치 범위(°). #4 논의 항목이라 첫 통합 데모(#36) 후 조정한다.
 * 최소가 0인 것은 "판정은 후하게" 원칙 때문이다. 보정으로 허용 오차를 좁히지 않는다.
 */
const val MIN_TOLERANCE_OFFSET_DEG = 0f
const val MAX_TOLERANCE_OFFSET_DEG = 15f

/**
 * 보정 화면의 프레임 하나.
 *
 * @property target 그 프레임에서 따라 하던 보정용 목표 자세
 * @property measured 그 프레임을 [angles]로 잰 관절 각도
 */
data class CalibrationSample(val target: TargetPose, val measured: Map<Joint, Float>)

/**
 * 보정 동작(오른손 들기 → 왼손 들기 → 다리 움직이기)에서 잰 각도로 허용 오차 보정치(°)를 정한다.
 *
 * [samples]는 #47 위치 안내가 OK인 동안 측정한 프레임이어야 한다.
 * 판정에서는 `target.toleranceDeg + 보정치`를 실제 허용 오차로 쓴다.
 *
 * 1. 샘플마다 목표 관절 중 측정값이 있고 NaN이 아닌 관절의 초과 오차 `|측정 − 목표| − toleranceDeg`를 구한다.
 * 2. (자세 id, 관절)별로 초과 오차의 중앙값을 구한다. 튀는 프레임에 흔들리지 않게 하기 위해서다.
 * 3. 그중 최댓값을 [MIN_TOLERANCE_OFFSET_DEG]~[MAX_TOLERANCE_OFFSET_DEG]로 자른다.
 *    오른손 들기처럼 한두 관절만 움직이는 자세에서도 그 관절의 체형 차이를 반영하기 위해 최댓값을 쓴다.
 *
 * 허용 오차보다 작게 어긋나면 0이다. 보정으로 허용 오차를 좁히지 않는다("판정은 후하게").
 *
 * @return 쓸 수 있는 관절이 하나도 없으면 0(보정 안 함)
 */
fun toleranceOffset(samples: List<CalibrationSample>): Float {
    val excessByJoint = mutableMapOf<Pair<String, Joint>, MutableList<Float>>()
    for (sample in samples) {
        for ((joint, target) in sample.target.targetAngles) {
            val measured = sample.measured[joint] ?: continue
            if (measured.isNaN()) continue
            excessByJoint.getOrPut(sample.target.id to joint) { mutableListOf() }
                .add(abs(measured - target) - sample.target.toleranceDeg)
        }
    }
    val worst = excessByJoint.values.maxOfOrNull { median(it) } ?: return 0f
    return worst.coerceIn(MIN_TOLERANCE_OFFSET_DEG, MAX_TOLERANCE_OFFSET_DEG)
}

private fun median(values: List<Float>): Float {
    val sorted = values.sorted()
    val middle = sorted.size / 2
    return if (sorted.size % 2 == 1) sorted[middle] else (sorted[middle - 1] + sorted[middle]) / 2
}
