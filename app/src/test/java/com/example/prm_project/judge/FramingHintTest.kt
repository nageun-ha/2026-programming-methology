package com.example.prm_project.judge

import com.example.prm_project.model.DetectionState
import com.example.prm_project.model.Joint
import com.example.prm_project.model.Landmark
import com.example.prm_project.model.PoseFrame
import org.junit.Assert.assertEquals
import org.junit.Test

class FramingHintTest {

    /** 코가 [top], 발목이 [bottom]에 있는 서 있는 사람. y는 0~1 정규화 값이다. */
    private fun standing(top: Float, bottom: Float, likelihood: Float = 0.9f): PoseFrame {
        val middle = (top + bottom) / 2
        val landmarks = mapOf(
            Joint.NOSE to Landmark(0.5f, top, likelihood),
            Joint.LEFT_HIP to Landmark(0.55f, middle, likelihood),
            Joint.RIGHT_HIP to Landmark(0.45f, middle, likelihood),
            Joint.LEFT_ANKLE to Landmark(0.55f, bottom, likelihood),
            Joint.RIGHT_ANKLE to Landmark(0.45f, bottom, likelihood),
        )
        return PoseFrame(landmarks, WIDTH, HEIGHT, 0L, DetectionState.DETECTED)
    }

    @Test
    fun `관절이 없으면 NOT_VISIBLE`() {
        val frame = PoseFrame(emptyMap(), WIDTH, HEIGHT, 0L, DetectionState.USER_CAUSED_LOSS)

        assertEquals(FramingHint.NOT_VISIBLE, judgeFraming(frame))
    }

    @Test
    fun `신뢰도가 전부 낮으면 LOW_LIGHT`() {
        assertEquals(FramingHint.LOW_LIGHT, judgeFraming(standing(0.1f, 0.9f, likelihood = 0.3f)))
    }

    @Test
    fun `몸이 화면 높이의 절반보다 작으면 COME_CLOSER`() {
        assertEquals(FramingHint.COME_CLOSER, judgeFraming(standing(0.4f, 0.7f))) // 박스 높이 0.3
    }

    @Test
    fun `SOFTWARE_LOSS면 직전 관절이 있어도 NOT_VISIBLE`() {
        val frame = standing(0.1f, 0.9f).copy(detection = DetectionState.SOFTWARE_LOSS)

        assertEquals(FramingHint.NOT_VISIBLE, judgeFraming(frame))
    }

    @Test
    fun `가운데 적정 크기면 OK`() {
        assertEquals(FramingHint.OK, judgeFraming(standing(0.1f, 0.9f))) // 박스 높이 0.8
    }

    private companion object {
        const val WIDTH = 480
        const val HEIGHT = 640
    }
}
