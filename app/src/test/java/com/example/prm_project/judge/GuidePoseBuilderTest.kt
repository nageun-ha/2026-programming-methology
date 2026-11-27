package com.example.prm_project.judge

import com.example.prm_project.model.Bone
import com.example.prm_project.model.DetectionState
import com.example.prm_project.model.Joint
import com.example.prm_project.model.Landmark
import com.example.prm_project.model.PoseFrame
import kotlin.math.atan2
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class GuidePoseBuilderTest {

    /** 480×640 이미지 픽셀 기준의 사람 모양. 팔은 좌우가 다르게 굽어 있다. */
    private val bodyPx = mapOf(
        Joint.NOSE to (240f to 140f),
        Joint.LEFT_SHOULDER to (200f to 200f), Joint.RIGHT_SHOULDER to (280f to 200f),
        Joint.LEFT_ELBOW to (170f to 260f), Joint.RIGHT_ELBOW to (320f to 240f),
        Joint.LEFT_WRIST to (150f to 320f), Joint.RIGHT_WRIST to (330f to 180f),
        Joint.LEFT_HIP to (215f to 340f), Joint.RIGHT_HIP to (265f to 340f),
        Joint.LEFT_KNEE to (210f to 430f), Joint.RIGHT_KNEE to (275f to 425f),
        Joint.LEFT_ANKLE to (205f to 520f), Joint.RIGHT_ANKLE to (285f to 515f),
    )

    /** [bodyPx]를 엉덩이 근처를 중심으로 [scale]배 한 프레임. [missing] 관절은 뺀다. */
    private fun person(scale: Float = 1f, missing: Set<Joint> = emptySet()) = PoseFrame(
        landmarks = (bodyPx - missing).mapValues { (_, p) ->
            Landmark((240 + (p.first - 240) * scale) / W, (340 + (p.second - 340) * scale) / H, 1f)
        },
        imageWidth = W, imageHeight = H, timestampMs = 0L, detection = DetectionState.DETECTED,
    )

    @Test
    fun `비정사각 이미지에서 뼈 방향은 픽셀 좌표로 잰다`() {
        val directions = boneDirections(person())

        assertEquals(-90f, directions.getValue(Bone.TORSO), EPS_DEG)
        val forearm = Math.toDegrees(atan2(320.0 - 260.0, 150.0 - 170.0)).toFloat()
        assertEquals(forearm, directions.getValue(Bone.LEFT_FOREARM), EPS_DEG)
    }

    @Test
    fun `잴 수 없는 뼈는 반대쪽 같은 뼈 길이로 대신한다`() {
        val frame = person(missing = setOf(Joint.LEFT_ELBOW))

        val lengths = boneLengths(frame)

        assertFalse(Bone.LEFT_UPPER_ARM in boneDirections(frame))
        assertEquals(lengths.getValue(Bone.RIGHT_UPPER_ARM), lengths.getValue(Bone.LEFT_UPPER_ARM), EPS_PX)
        assertEquals(lengths.getValue(Bone.RIGHT_FOREARM), lengths.getValue(Bone.LEFT_FOREARM), EPS_PX)
    }

    private companion object {
        const val W = 480
        const val H = 640
        const val EPS_PX = 1e-2f
        const val EPS_DEG = 1e-2f
    }
}
