package com.example.prm_project.pose

import com.example.prm_project.model.Joint
import com.example.prm_project.model.Landmark
import org.junit.Assert.assertEquals
import org.junit.Test

class FrameMapperTest {

    // 세로로 든 폰의 전면 카메라: 가로 640×480 버퍼를 시계 방향 270° 돌려야 똑바로 선다
    private val front = FrameMapper(bufferWidth = 640, bufferHeight = 480, rotationDegrees = 270, mirror = true)

    private fun point(x: Float, y: Float, rotation: Int, mirror: Boolean = false) =
        FrameMapper(640, 480, rotation, mirror)
            .map(listOf(RawLandmark(BLAZE_NOSE, x, y, 1f)))
            .getValue(Joint.NOSE).let { it.x to it.y }

    @Test
    fun `회전 270도 거울 반전`() {
        val result = front.map(listOf(RawLandmark(BLAZE_NOSE, 160f, 120f, 0.9f)))

        // 0~1: (0.25, 0.25) → 270° 회전: (0.25, 0.75) → 거울: (0.75, 0.75)
        assertEquals(Landmark(0.75f, 0.75f, 0.9f), result[Joint.NOSE])
        assertEquals(480, front.imageWidth)
        assertEquals(640, front.imageHeight)
    }

    @Test
    fun `회전 0 90 180`() {
        assertEquals(0.25f to 0.25f, point(160f, 120f, 0))
        assertEquals(0.75f to 0.25f, point(160f, 120f, 90))
        assertEquals(0.75f to 0.75f, point(160f, 120f, 180))
        assertEquals(0.75f to 0.25f, point(160f, 120f, 0, mirror = true))
    }

    @Test
    fun `화면 밖 좌표는 자르지 않고 판정에 없는 점은 버린다`() {
        val mapper = FrameMapper(100, 100, 0, mirror = false)
        val result = mapper.map(
            listOf(
                RawLandmark(16, 120f, 50f, 0.2f), // 오른손목이 오른쪽으로 화면 밖
                RawLandmark(2, 50f, 50f, 1f), // 왼눈: Joint에 없음
            ),
        )

        assertEquals(mapOf(Joint.RIGHT_WRIST to Landmark(1.2f, 0.5f, 0.2f)), result)
    }

    @Test
    fun `BlazePose 번호가 몸 기준 관절 이름과 맞다`() {
        val names = BLAZE_POSE_JOINTS.entries.associate { (index, joint) -> joint to index }

        assertEquals(Joint.entries.size, names.size)
        assertEquals(15, names[Joint.LEFT_WRIST])
        assertEquals(16, names[Joint.RIGHT_WRIST])
        assertEquals(28, names[Joint.RIGHT_ANKLE])
    }

    private companion object {
        const val BLAZE_NOSE = 0
    }
}
