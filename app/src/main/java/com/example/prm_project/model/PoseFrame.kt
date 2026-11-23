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
 */
data class PoseFrame(
    val landmarks: Map<Joint, Landmark>,
    val imageWidth: Int,
    val imageHeight: Int,
    val timestampMs: Long,
    val detection: DetectionState,
)
