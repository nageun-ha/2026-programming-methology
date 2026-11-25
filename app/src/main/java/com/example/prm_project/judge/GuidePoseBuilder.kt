package com.example.prm_project.judge

import com.example.prm_project.model.Bone
import com.example.prm_project.model.GuidePoint
import com.example.prm_project.model.GuidePose
import com.example.prm_project.model.Joint
import com.example.prm_project.model.PoseFrame
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

private data class Px(val x: Double, val y: Double) {
    operator fun plus(o: Px) = Px(x + o.x, y + o.y)
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

/**
 * 목표 뼈 방향 [directions]를 [frame] 사용자의 체형에 맞춰 관절 위치로 바꾼다.
 *
 * - 엉덩이 중점에서 시작해 `부모 위치 + 목표 방향 × 사용자 뼈 길이`로 차례로 계산한다.
 * - 좌우 엉덩이와 어깨는 사용자 실제 중점 기준 위치를 그대로 쓴다.
 * - 방향이나 길이가 없는 뼈는 그 뼈와 그 아래 관절을 뺀다.
 *
 * @return 엉덩이가 하나라도 없으면 null이다.
 */
fun buildGuidePose(directions: Map<Bone, Float>, frame: PoseFrame): GuidePose? {
    val px = frame.toPx()
    val hips = Bone.TORSO.from
    val hipCenter = px.at(hips) ?: return null
    val lengths = boneLengths(frame)
    val guide = hips.associateWith { px.getValue(it) }.toMutableMap()

    val shoulderCenter = px.at(Bone.TORSO.to)
    val torsoEnd = step(hipCenter, directions[Bone.TORSO], lengths[Bone.TORSO])
    if (shoulderCenter != null && torsoEnd != null) {
        Bone.TORSO.to.forEach { guide[it] = torsoEnd + (px.getValue(it) - shoulderCenter) }
    }
    for (bone in Bone.entries - Bone.TORSO) {
        val end = step(guide.at(bone.from) ?: continue, directions[bone], lengths[bone]) ?: continue
        guide[bone.to.single()] = end
    }
    return GuidePose(guide.mapValues { (_, p) -> GuidePoint((p.x / frame.imageWidth).toFloat(), (p.y / frame.imageHeight).toFloat()) })
}

/** [start]에서 [degrees] 방향으로 [length]만큼 간 위치. 방향이나 길이가 없으면 null이다. */
private fun step(start: Px, degrees: Float?, length: Float?): Px? {
    if (degrees == null || length == null) return null
    val rad = Math.toRadians(degrees.toDouble())
    return start + Px(cos(rad) * length, sin(rad) * length)
}
