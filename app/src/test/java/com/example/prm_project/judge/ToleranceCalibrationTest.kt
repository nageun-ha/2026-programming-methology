package com.example.prm_project.judge

import com.example.prm_project.model.Joint
import com.example.prm_project.model.TargetPose
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ToleranceCalibrationTest {

    private val raiseRight = TargetPose(
        id = "calib_raise_right",
        name = "오른손 들기",
        targetAngles = mapOf(
            Joint.RIGHT_SHOULDER to 170f,
            Joint.RIGHT_ELBOW to 180f,
            Joint.LEFT_HIP to 180f,
            Joint.LEFT_KNEE to 180f,
        ),
        toleranceDeg = TOLERANCE,
    )

    /** 모든 목표 관절이 [offBy]°만큼 덜 펴진 프레임 */
    private fun sample(offBy: Float, pose: TargetPose = raiseRight) =
        CalibrationSample(pose, pose.targetAngles.mapValues { it.value - offBy })

    private fun frames(count: Int, offBy: Float) = List(count) { sample(offBy) }

    @Test
    fun `체형 차이로 모든 각도가 10도 어긋나면 보정치가 범위 안`() {
        val offset = toleranceOffset(frames(10, 10f))

        assertTrue(offset in MIN_TOLERANCE_OFFSET_DEG..MAX_TOLERANCE_OFFSET_DEG)
    }

    @Test
    fun `오차가 허용 오차보다 작으면 좁히지 않고 0`() {
        assertEquals(0f, toleranceOffset(frames(10, 5f)), DELTA)
    }

    @Test
    fun `허용 오차를 10도 넘는 오차가 꾸준하면 약 10`() {
        assertEquals(10f, toleranceOffset(frames(10, TOLERANCE + 10f)), DELTA)
    }

    @Test
    fun `아주 큰 오차는 최대 보정치로 자른다`() {
        assertEquals(MAX_TOLERANCE_OFFSET_DEG, toleranceOffset(frames(10, 80f)), DELTA)
    }

    @Test
    fun `한 관절만 꾸준히 어긋나도 그만큼 넓힌다`() {
        val shoulderOnly = List(10) {
            CalibrationSample(raiseRight, raiseRight.targetAngles + (Joint.RIGHT_SHOULDER to 170f - TOLERANCE - 10f))
        }

        assertEquals(10f, toleranceOffset(shoulderOnly), DELTA)
    }

    @Test
    fun `프레임 하나만 크게 튀면 결과가 거의 바뀌지 않는다`() {
        val steady = frames(9, TOLERANCE + 10f)

        assertEquals(10f, toleranceOffset(steady + sample(90f)), DELTA)
    }

    @Test
    fun `측정값이 없거나 NaN인 관절은 무시한다`() {
        val measured = mapOf(
            Joint.RIGHT_SHOULDER to 170f - TOLERANCE - 10f,
            Joint.RIGHT_ELBOW to Float.NaN,
            Joint.NOSE to 0f, // 목표에 없는 관절
        )

        assertEquals(10f, toleranceOffset(List(5) { CalibrationSample(raiseRight, measured) }), DELTA)
    }

    @Test
    fun `빈 입력이나 쓸 수 있는 관절이 없으면 0`() {
        val nothingUsable = CalibrationSample(raiseRight, mapOf(Joint.RIGHT_ELBOW to Float.NaN))

        assertEquals(0f, toleranceOffset(emptyList()), DELTA)
        assertEquals(0f, toleranceOffset(listOf(nothingUsable)), DELTA)
    }

    private companion object {
        const val TOLERANCE = 15f
        const val DELTA = 0.01f
    }
}
