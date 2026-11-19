package com.example.prm_project.pose

import com.example.prm_project.model.Joint
import com.example.prm_project.model.Landmark

/**
 * 포즈 라이브러리가 낸 점 하나.
 *
 * @property index BlazePose 33점 번호. ML Kit `PoseLandmark` 상수와 MediaPipe 랜드마크 순서가 같다.
 * @property x 카메라 버퍼(회전 전) 기준 픽셀
 * @property likelihood 화면 안에 보일 확률(ML Kit `inFrameLikelihood`, MediaPipe `visibility`)
 */
data class RawLandmark(val index: Int, val x: Float, val y: Float, val likelihood: Float)

/** BlazePose 번호 → [Joint]. 라이브러리의 LEFT/RIGHT도 사용자 몸 기준이라 이름을 그대로 옮긴다. */
val BLAZE_POSE_JOINTS: Map<Int, Joint> = mapOf(
    0 to Joint.NOSE,
    11 to Joint.LEFT_SHOULDER, 12 to Joint.RIGHT_SHOULDER,
    13 to Joint.LEFT_ELBOW, 14 to Joint.RIGHT_ELBOW,
    15 to Joint.LEFT_WRIST, 16 to Joint.RIGHT_WRIST,
    23 to Joint.LEFT_HIP, 24 to Joint.RIGHT_HIP,
    25 to Joint.LEFT_KNEE, 26 to Joint.RIGHT_KNEE,
    27 to Joint.LEFT_ANKLE, 28 to Joint.RIGHT_ANKLE,
)

/**
 * 카메라 버퍼 픽셀 좌표를 [com.example.prm_project.model.PoseFrame] 좌표 규칙(0~1, 화면·거울 기준)으로 바꾼다.
 *
 * 순서: 픽셀 → 0~1 → [rotationDegrees]만큼 시계 방향 회전 → [mirror]면 x만 반전. 관절 이름은 바꾸지 않는다.
 * 화면 밖 좌표는 0~1을 벗어난 채로 둔다. 화면 밖인지는 [DetectionClassifier]가 이 값으로 판단한다.
 *
 * @param rotationDegrees CameraX `imageInfo.rotationDegrees`. 라이브러리가 이미 회전한 좌표를 주면 0과 회전 후 크기를 넣는다.
 * @param mirror 전면 카메라면 true. 미리보기(PreviewView)가 전면 영상을 거울처럼 보여 주므로 맞춘다.
 */
class FrameMapper(
    private val bufferWidth: Int,
    private val bufferHeight: Int,
    private val rotationDegrees: Int,
    private val mirror: Boolean,
) {
    private val sideways = rotationDegrees % 180 != 0

    /** 회전 후 이미지 크기. [com.example.prm_project.model.PoseFrame.imageWidth]에 넣는다. */
    val imageWidth = if (sideways) bufferHeight else bufferWidth
    val imageHeight = if (sideways) bufferWidth else bufferHeight

    fun map(raw: List<RawLandmark>): Map<Joint, Landmark> = raw.mapNotNull { point ->
        val joint = BLAZE_POSE_JOINTS[point.index] ?: return@mapNotNull null
        val u = point.x / bufferWidth
        val v = point.y / bufferHeight
        val (x, y) = when (rotationDegrees) {
            90 -> 1 - v to u
            180 -> 1 - u to 1 - v
            270 -> v to 1 - u
            else -> u to v
        }
        joint to Landmark(if (mirror) 1 - x else x, y, point.likelihood)
    }.toMap()
}
