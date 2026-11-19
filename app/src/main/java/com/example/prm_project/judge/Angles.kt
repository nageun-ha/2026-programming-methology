package com.example.prm_project.judge

import com.example.prm_project.model.Joint
import com.example.prm_project.model.Landmark
import com.example.prm_project.model.PoseFrame
import kotlin.math.acos
import kotlin.math.sqrt

/**
 * 판정에 쓰는 관절 각도 목록.
 *
 * 키는 각도의 꼭짓점 관절이고, 값은 꼭짓점에서 뻗는 양 끝 관절이다.
 * 키는 [com.example.prm_project.model.TargetPose.targetAngles]의 키와 같다.
 */
val JOINT_ANGLES: Map<Joint, Pair<Joint, Joint>> = mapOf(
    Joint.LEFT_ELBOW to (Joint.LEFT_SHOULDER to Joint.LEFT_WRIST),
    Joint.RIGHT_ELBOW to (Joint.RIGHT_SHOULDER to Joint.RIGHT_WRIST),
    Joint.LEFT_SHOULDER to (Joint.LEFT_HIP to Joint.LEFT_ELBOW),
    Joint.RIGHT_SHOULDER to (Joint.RIGHT_HIP to Joint.RIGHT_ELBOW),
    Joint.LEFT_HIP to (Joint.LEFT_SHOULDER to Joint.LEFT_KNEE),
    Joint.RIGHT_HIP to (Joint.RIGHT_SHOULDER to Joint.RIGHT_KNEE),
    Joint.LEFT_KNEE to (Joint.LEFT_HIP to Joint.LEFT_ANKLE),
    Joint.RIGHT_KNEE to (Joint.RIGHT_HIP to Joint.RIGHT_ANKLE),
)

/**
 * 세 관절이 이루는 각도를 잰다. [b]가 꼭짓점이다.
 *
 * 정규화 좌표의 x에 [width], y에 [height]를 곱해 픽셀 좌표로 바꾼 뒤 계산한다.
 * 비정사각 이미지에서 정규화 좌표로 바로 재면 각도가 틀리기 때문이다.
 *
 * @return 0~180°. [a]나 [c]가 [b]와 같은 위치라 벡터 길이가 0이면 [Float.NaN]이다.
 */
fun angle(a: Landmark, b: Landmark, c: Landmark, width: Int, height: Int): Float {
    val ax = ((a.x - b.x) * width).toDouble()
    val ay = ((a.y - b.y) * height).toDouble()
    val cx = ((c.x - b.x) * width).toDouble()
    val cy = ((c.y - b.y) * height).toDouble()

    val lengthA = sqrt(ax * ax + ay * ay)
    val lengthC = sqrt(cx * cx + cy * cy)
    if (lengthA == 0.0 || lengthC == 0.0) return Float.NaN

    val cos = ((ax * cx + ay * cy) / (lengthA * lengthC)).coerceIn(-1.0, 1.0)
    return Math.toDegrees(acos(cos)).toFloat()
}

/**
 * [frame]에서 잴 수 있는 [JOINT_ANGLES]의 각도를 모두 잰다.
 *
 * 세 관절 중 하나라도 잡히지 않았거나 각도가 [Float.NaN]이면 그 항목은 결과에 없다.
 */
fun angles(frame: PoseFrame): Map<Joint, Float> {
    val result = mutableMapOf<Joint, Float>()
    for ((vertex, ends) in JOINT_ANGLES) {
        val a = frame.landmarks[ends.first] ?: continue
        val b = frame.landmarks[vertex] ?: continue
        val c = frame.landmarks[ends.second] ?: continue
        val degrees = angle(a, b, c, frame.imageWidth, frame.imageHeight)
        if (!degrees.isNaN()) result[vertex] = degrees
    }
    return result
}
