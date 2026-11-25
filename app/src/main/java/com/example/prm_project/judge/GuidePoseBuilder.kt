package com.example.prm_project.judge

import com.example.prm_project.model.Bone
import com.example.prm_project.model.Joint
import com.example.prm_project.model.PoseFrame
import kotlin.math.atan2
import kotlin.math.hypot

private data class Px(val x: Double, val y: Double) {
    operator fun minus(o: Px) = Px(x - o.x, y - o.y)
}

private fun PoseFrame.toPx() = landmarks.mapValues { (_, l) -> Px(l.x.toDouble() * imageWidth, l.y.toDouble() * imageHeight) }

/** [joints]의 중점. 하나라도 없으면 null이다. */
private fun Map<Joint, Px>.at(joints: List<Joint>): Px? {
    val found = joints.map { this[it] ?: return null }
    return Px(found.sumOf { it.x } / found.size, found.sumOf { it.y } / found.size)
}

/** 좌우를 바꾼 같은 뼈. 몸 가운데 뼈는 자기 자신이다. */
private val Bone.mirror: Bone
    get() = Bone.valueOf(name.replace("LEFT", "#").replace("RIGHT", "LEFT").replace("#", "RIGHT"))

/** [frame]에서 잴 수 있는 뼈 방향(°)을 모두 잰다. 기준은 [Bone] 참고. 길이가 0인 뼈는 뺀다. */
fun boneDirections(frame: PoseFrame): Map<Bone, Float> {
    val px = frame.toPx()
    return Bone.entries.mapNotNull { bone ->
        val d = (px.at(bone.to) ?: return@mapNotNull null) - (px.at(bone.from) ?: return@mapNotNull null)
        if (d.x == 0.0 && d.y == 0.0) null else bone to Math.toDegrees(atan2(d.y, d.x)).toFloat()
    }.toMap()
}

/** 뼈 길이(픽셀). 잴 수 없으면 반대쪽 같은 뼈 길이로 대신하고, 그것도 없으면 뺀다. */
internal fun boneLengths(frame: PoseFrame): Map<Bone, Float> {
    val px = frame.toPx()
    val measured = Bone.entries.mapNotNull { bone ->
        val d = (px.at(bone.to) ?: return@mapNotNull null) - (px.at(bone.from) ?: return@mapNotNull null)
        bone to hypot(d.x, d.y).toFloat()
    }.toMap()
    return Bone.entries.mapNotNull { bone -> (measured[bone] ?: measured[bone.mirror])?.let { bone to it } }.toMap()
}
