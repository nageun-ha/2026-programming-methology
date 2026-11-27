package com.example.prm_project.model

/**
 * 관절 하나의 위치.
 *
 * @property x 0~1로 정규화한 가로 좌표
 * @property y 0~1로 정규화한 세로 좌표
 * @property likelihood 관절이 화면 안에 보일 확률(0~1). 라이브러리마다 다른 값을 ①이 이 뜻으로 맞춘다.
 */
data class Landmark(val x: Float, val y: Float, val likelihood: Float)

/**
 * 포즈 인식(①)이 한 프레임마다 넘기는 결과.
 *
 * - 좌표는 0~1로 정규화하고 화면(거울) 기준이다. 사용자가 미리보기에서 보는 모습 그대로다.
 * - 관절 이름은 사용자 몸 기준이다. [Joint.LEFT_WRIST]는 실제 왼손목이다.
 * - 반전·회전 보정은 ①이 한 곳에서 처리한다. ②·③은 받은 값을 그대로 쓴다.
 * - 비정사각 이미지에서는 각도를 계산하기 전에 x에 [imageWidth], y에 [imageHeight]를 곱해야 한다.
 *
 * @property landmarks 화면 안(0~1)에 있고 신뢰도가 기준 이상인 관절만 들어 있다. 기준은 ①이 정한다.
 *   [detection]이 [DetectionState.SOFTWARE_LOSS]면 직전에 잡힌 관절이 그대로 들어 있다(스켈레톤 깜빡임 방지).
 * @property imageWidth 회전 보정 후 이미지 너비(px). 세로로 든 폰이면 높이보다 작다.
 * @property timestampMs ①이 프레임을 받은 시각(ms). [android.os.SystemClock.elapsedRealtime] 기준이라
 *   운동 시작 시각(`ExerciseState.startedAtMs`)과 같은 시계로 비교할 수 있다(#45).
 * @property outOfFrame 이번 프레임에서 좌표가 화면(0~1) 밖으로 추정된 관절. 신뢰도와 상관없다. [landmarks]에는 없다.
 *   경계를 넘은 정도가 작으면(추정 흔들림 범위) [detection]은 DETECTED로 남는다.
 * @property lowConfidence 이번 프레임에서 화면 안에 있지만 신뢰도가 기준 미만인 관절(가려짐·어두움). [landmarks]에는 없다.
 * @property edge 방향별 화면 잘림 정도(#50)
 */
data class PoseFrame(
    val landmarks: Map<Joint, Landmark>,
    val imageWidth: Int,
    val imageHeight: Int,
    val timestampMs: Long,
    val detection: DetectionState,
    val outOfFrame: Set<Joint> = emptySet(),
    val lowConfidence: Set<Joint> = emptySet(),
    val edge: EdgeWarning = EdgeWarning.NONE,
)

/**
 * 화면 가장자리 방향별 잘림 정도. 각 0~1이고 방향은 화면(거울) 기준이다.
 *
 * 그 방향으로 가장 많이 나간 관절 기준으로, 가장자리 안쪽 5%에서 0부터 올라 경계 밖 5%에서 1이 된다.
 * 소프트웨어 원인 인식 실패면 모두 0이고, 관절이 하나도 없으면 직전 값을 유지한다.
 * 하나의 값만 필요하면 `maxOf(left, right, top, bottom)`을 쓴다.
 */
data class EdgeWarning(val left: Float, val right: Float, val top: Float, val bottom: Float) {
    companion object {
        val NONE = EdgeWarning(0f, 0f, 0f, 0f)
    }
}
