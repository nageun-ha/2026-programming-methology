package com.example.prm_project.judge

import com.example.prm_project.model.DetectionState
import com.example.prm_project.model.Joint
import com.example.prm_project.model.Landmark
import com.example.prm_project.model.PoseFrame
import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AnglesTest {

    private fun point(x: Float, y: Float) = Landmark(x, y, 1f)

    private fun pixel(x: Float, y: Float) = point(x / WIDTH, y / HEIGHT)

    private fun mirrored(p: Landmark) = p.copy(x = 1f - p.x)

    @Test
    fun `직각이면 90도`() {
        val result = angle(point(0.5f, 0.2f), point(0.5f, 0.5f), point(0.8f, 0.5f), 100, 100)

        assertEquals(90f, result, DELTA)
    }

    @Test
    fun `일직선이면 180도`() {
        val result = angle(point(0.2f, 0.2f), point(0.5f, 0.5f), point(0.8f, 0.8f), 100, 100)

        assertEquals(180f, result, DELTA)
    }

    @Test
    fun `480x640 이미지에서 픽셀 기준 직각이면 90도`() {
        val a = pixel(360f, 440f)
        val b = pixel(240f, 320f)
        val c = pixel(120f, 440f)

        assertEquals(90f, angle(a, b, c, WIDTH, HEIGHT), DELTA)
        assertTrue(abs(angle(a, b, c, 1, 1) - 90f) > 1f)
    }

    @Test
    fun `거울 반전해도 각도가 같다`() {
        val a = pixel(100f, 150f)
        val b = pixel(240f, 320f)
        val c = pixel(400f, 300f)

        val original = angle(a, b, c, WIDTH, HEIGHT)
        val flipped = angle(mirrored(a), mirrored(b), mirrored(c), WIDTH, HEIGHT)

        assertEquals(original, flipped, DELTA)
    }

    @Test
    fun `관절 하나가 빠지면 그 관절이 들어간 각도만 빠진다`() {
        val landmarks = mapOf(
            Joint.LEFT_HIP to pixel(240f, 400f),
            Joint.LEFT_SHOULDER to pixel(240f, 200f),
            Joint.LEFT_ELBOW to pixel(340f, 200f),
            Joint.LEFT_WRIST to pixel(340f, 100f),
        )
        val full = PoseFrame(landmarks, WIDTH, HEIGHT, 0L, DetectionState.DETECTED)
        val withoutWrist = full.copy(landmarks = landmarks - Joint.LEFT_WRIST)

        assertEquals(setOf(Joint.LEFT_SHOULDER, Joint.LEFT_ELBOW), angles(full).keys)
        assertEquals(90f, angles(full).getValue(Joint.LEFT_ELBOW), DELTA)
        assertFalse(Joint.LEFT_ELBOW in angles(withoutWrist))
        assertEquals(90f, angles(withoutWrist).getValue(Joint.LEFT_SHOULDER), DELTA)
    }

    private companion object {
        const val WIDTH = 480
        const val HEIGHT = 640
        const val DELTA = 0.01f
    }
}
