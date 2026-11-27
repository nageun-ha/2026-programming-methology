package com.example.prm_project.judge

import com.example.prm_project.model.DetectionState
import com.example.prm_project.model.EdgeWarning
import com.example.prm_project.model.Joint
import com.example.prm_project.model.Landmark
import com.example.prm_project.model.PoseFrame
import org.junit.Assert.assertEquals
import org.junit.Test

class FramingHintTest {

    /** 가운데 적정 크기로 서 있고, 가장자리 잘림 정도만 [EdgeWarning]으로 준 프레임. */
    private fun cut(left: Float = 0f, right: Float = 0f, top: Float = 0f, bottom: Float = 0f) =
        standing(0.1f, 0.9f).copy(edge = EdgeWarning(left, right, top, bottom))

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
    fun `SOFTWARE_LOSS에서 저신뢰 관절이 있으면 LOW_LIGHT`() {
        val lowConfidence = setOf(Joint.NOSE, Joint.LEFT_HIP)
        val softwareLoss = standing(0.1f, 0.9f).copy(detection = DetectionState.SOFTWARE_LOSS, lowConfidence = lowConfidence)
        val empty = PoseFrame(emptyMap(), WIDTH, HEIGHT, 0L, DetectionState.USER_CAUSED_LOSS, lowConfidence = lowConfidence)

        assertEquals(FramingHint.LOW_LIGHT, judgeFraming(softwareLoss))
        assertEquals(FramingHint.LOW_LIGHT, judgeFraming(empty))
    }

    @Test
    fun `화면 왼쪽이 잘리면 MOVE_RIGHT, 오른쪽이 잘리면 MOVE_LEFT`() {
        assertEquals(FramingHint.MOVE_RIGHT, judgeFraming(cut(left = 0.6f)))
        assertEquals(FramingHint.MOVE_LEFT, judgeFraming(cut(right = 0.6f)))
    }

    @Test
    fun `위나 아래가 잘리거나 좌우가 둘 다 잘리면 STEP_BACK`() {
        assertEquals(FramingHint.STEP_BACK, judgeFraming(cut(top = 0.6f)))
        assertEquals(FramingHint.STEP_BACK, judgeFraming(cut(bottom = 0.6f)))
        assertEquals(FramingHint.STEP_BACK, judgeFraming(cut(left = 0.6f, right = 0.6f)))
    }

    @Test
    fun `몸이 화면 높이의 절반보다 작으면 COME_CLOSER`() {
        assertEquals(FramingHint.COME_CLOSER, judgeFraming(standing(0.4f, 0.7f))) // 박스 높이 0.3
    }

    @Test
    fun `SOFTWARE_LOSS에서 저신뢰 관절이 없으면 직전 관절이 있어도 NOT_VISIBLE`() {
        val frame = standing(0.1f, 0.9f).copy(detection = DetectionState.SOFTWARE_LOSS)

        assertEquals(FramingHint.NOT_VISIBLE, judgeFraming(frame))
    }

    @Test
    fun `가장자리 값이 기준 바로 아래이고 가운데 적정 크기면 OK`() {
        val nearEdge = cut(left = 0.49f, right = 0.49f, top = 0.49f, bottom = 0.49f) // 기준 0.5 바로 아래

        assertEquals(FramingHint.OK, judgeFraming(standing(0.1f, 0.9f))) // 박스 높이 0.8
        assertEquals(FramingHint.OK, judgeFraming(nearEdge))
    }

    private companion object {
        const val WIDTH = 480
        const val HEIGHT = 640
    }
}
