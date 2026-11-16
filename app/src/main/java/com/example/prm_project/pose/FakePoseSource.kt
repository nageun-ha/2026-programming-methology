package com.example.prm_project.pose

import com.example.prm_project.model.DetectionState
import com.example.prm_project.model.Joint
import com.example.prm_project.model.Landmark
import com.example.prm_project.model.PoseFrame
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * 카메라·포즈 라이브러리 없이 판정(②)과 화면(③)을 개발하기 위한 가짜 [PoseSource].
 *
 * [fps]로 아래 5.3초 시나리오를 끝없이 반복한다. 좌표는 [PoseFrame] 규칙(0~1, 거울 기준)을 따르므로
 * 사용자 오른쪽 관절이 화면 오른쪽(x > 0.5)에 있다.
 *
 * | 구간(초) | 자세 | 감지 상태 |
 * |---|---|---|
 * | 0~2 | 서 있기 | DETECTED |
 * | 2~4 | 오른손 들기 | DETECTED |
 * | 4~5 | 화면 밖 (관절 없음) | USER_CAUSED_LOSS |
 * | 5~5.3 | 서 있기 좌표 유지 | SOFTWARE_LOSS |
 *
 * [frames]는 cold flow라 수집할 때마다 0ms부터 시작한다. [PoseFrame.timestampMs]는 수집 시작 기준 경과 시간이다.
 */
class FakePoseSource(private val fps: Int = 30) : PoseSource {

    override val frames: Flow<PoseFrame> = flow {
        var index = 0L
        while (true) {
            val t = index * 1000 / fps
            emit(frameAt(t))
            index++
            delay(index * 1000 / fps - t) // 누적 오차 없이 index로 시각을 정한다
        }
    }

    internal fun frameAt(timestampMs: Long): PoseFrame {
        val (landmarks, detection) = when (timestampMs % CYCLE_MS) {
            in 0L until 2000L -> STANDING to DetectionState.DETECTED
            in 2000L until 4000L -> RIGHT_HAND_UP to DetectionState.DETECTED
            in 4000L until 5000L -> emptyMap<Joint, Landmark>() to DetectionState.USER_CAUSED_LOSS
            else -> STANDING to DetectionState.SOFTWARE_LOSS
        }
        return PoseFrame(landmarks, IMAGE_WIDTH, IMAGE_HEIGHT, timestampMs, detection)
    }

    private companion object {
        const val CYCLE_MS = 5300L
        const val IMAGE_WIDTH = 480
        const val IMAGE_HEIGHT = 640

        fun lm(x: Float, y: Float) = Landmark(x, y, likelihood = 0.99f)

        val STANDING = mapOf(
            Joint.NOSE to lm(0.50f, 0.20f),
            Joint.LEFT_SHOULDER to lm(0.40f, 0.30f), Joint.RIGHT_SHOULDER to lm(0.60f, 0.30f),
            Joint.LEFT_ELBOW to lm(0.38f, 0.42f), Joint.RIGHT_ELBOW to lm(0.62f, 0.42f),
            Joint.LEFT_WRIST to lm(0.37f, 0.53f), Joint.RIGHT_WRIST to lm(0.63f, 0.53f),
            Joint.LEFT_HIP to lm(0.45f, 0.55f), Joint.RIGHT_HIP to lm(0.55f, 0.55f),
            Joint.LEFT_KNEE to lm(0.45f, 0.72f), Joint.RIGHT_KNEE to lm(0.55f, 0.72f),
            Joint.LEFT_ANKLE to lm(0.45f, 0.88f), Joint.RIGHT_ANKLE to lm(0.55f, 0.88f),
        )

        val RIGHT_HAND_UP = STANDING + mapOf(
            Joint.RIGHT_ELBOW to lm(0.64f, 0.20f),
            Joint.RIGHT_WRIST to lm(0.66f, 0.10f),
        )
    }
}
