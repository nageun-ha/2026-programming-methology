package com.example.prm_project.pose

import com.example.prm_project.model.DetectionState.DETECTED
import com.example.prm_project.model.DetectionState.SOFTWARE_LOSS
import com.example.prm_project.model.DetectionState.USER_CAUSED_LOSS
import com.example.prm_project.model.EdgeWarning
import com.example.prm_project.model.Joint
import com.example.prm_project.model.Landmark
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DetectionClassifierTest {

    private val classifier = DetectionClassifier()
    private var t = 0L

    /** 30fps 한 프레임 넣기 */
    private fun feed(raw: Map<Joint, Landmark>) = classifier.classify(raw, t).also { t += 33 }

    /** 화면 가운데 선 사람을 dx만큼 옆으로 옮긴 관절 */
    private fun body(dx: Float = 0f, likelihood: Float = 0.9f) = mapOf(
        Joint.NOSE to Landmark(0.50f + dx, 0.15f, likelihood),
        Joint.LEFT_SHOULDER to Landmark(0.40f + dx, 0.30f, likelihood),
        Joint.RIGHT_SHOULDER to Landmark(0.60f + dx, 0.30f, likelihood),
        Joint.RIGHT_WRIST to Landmark(0.63f + dx, 0.53f, likelihood),
        Joint.LEFT_ANKLE to Landmark(0.45f + dx, 0.88f, likelihood),
        Joint.RIGHT_ANKLE to Landmark(0.55f + dx, 0.88f, likelihood),
    )

    @Test
    fun `가장자리로 걸어 나가면 사용자 원인`() {
        assertEquals(DETECTED, feed(body()).state)
        assertEquals(DETECTED, feed(body(dx = 0.35f)).state) // 오른손목 x=0.98, 가장자리에 닿음

        val partly = feed(body(dx = 0.45f)) // 오른손목 x=1.08, 확실히 화면 밖
        assertEquals(USER_CAUSED_LOSS, partly.state)
        assertTrue(partly.landmarks.values.all { it.x <= 1f })

        val gone = feed(emptyMap())
        assertEquals(USER_CAUSED_LOSS, gone.state)
        assertTrue(gone.landmarks.isEmpty())
    }

    @Test
    fun `가운데서 3프레임 끊기면 소프트웨어 원인이고 직전 좌표 유지`() {
        val last = feed(body()).landmarks
        repeat(3) {
            val result = feed(emptyMap())
            assertEquals(SOFTWARE_LOSS, result.state)
            assertEquals(last, result.landmarks)
        }
        assertEquals(DETECTED, feed(body()).state)
    }

    @Test
    fun `끊긴 지 1000ms까지 소프트웨어 원인, 1001ms부터 사용자 원인`() {
        classifier.classify(body(), 0)
        classifier.classify(emptyMap(), 10)
        assertEquals(SOFTWARE_LOSS, classifier.classify(emptyMap(), 1010).state)
        assertEquals(USER_CAUSED_LOSS, classifier.classify(emptyMap(), 1011).state)
    }

    @Test
    fun `화면 안인데 신뢰도가 전부 낮으면 소프트웨어 원인`() {
        feed(body())
        assertEquals(SOFTWARE_LOSS, feed(body(likelihood = 0.1f)).state)
    }

    @Test
    fun `처음부터 아무도 없으면 사용자 원인`() {
        assertEquals(USER_CAUSED_LOSS, feed(emptyMap()).state)
    }

    // PR #46 리뷰에서 나온 재현 사례

    @Test
    fun `저신뢰 관절 하나가 경계를 살짝 넘어도 정상 감지`() {
        val result = feed(body() + (Joint.LEFT_WRIST to Landmark(-0.02f, 0.5f, 0.1f)))

        assertEquals(DETECTED, result.state)
        assertFalse(Joint.LEFT_WRIST in result.landmarks)
    }

    @Test
    fun `머리가 위쪽 가장자리에 닿아 있어도 짧은 끊김은 소프트웨어 원인`() {
        feed(body() + (Joint.NOSE to Landmark(0.5f, 0.03f, 0.9f)))
        assertEquals(SOFTWARE_LOSS, feed(emptyMap()).state)
    }

    @Test
    fun `가운데서 한 번에 화면 밖으로 사라지면 사용자 원인`() {
        feed(body())
        assertEquals(USER_CAUSED_LOSS, feed(body(dx = 1f, likelihood = 0.1f)).state)
    }

    // #50 방향별 잘림 정도, outOfFrame, lowConfidence

    @Test
    fun `가장자리로 갈수록 그쪽 잘림 정도가 오르고 화면 밖 관절은 outOfFrame`() {
        val center = feed(body())
        assertEquals(EdgeWarning.NONE, center.edge)
        assertTrue(center.outOfFrame.isEmpty())

        val near = feed(body(dx = 0.35f)) // 오른손목 x=0.98 → (0.05 − 0.02) / 0.1
        assertEquals(0.3f, near.edge.right, 1e-4f)
        assertEquals(0f, near.edge.left)

        val out = feed(body(dx = 0.45f)) // 오른손목 x=1.08, 오른어깨 x=1.05
        assertEquals(1f, out.edge.right)
        assertEquals(setOf(Joint.RIGHT_SHOULDER, Joint.RIGHT_WRIST), out.outOfFrame)
        assertFalse(Joint.RIGHT_WRIST in out.landmarks)
    }

    @Test
    fun `소프트웨어 원인이면 잘림 정도 0, 사용자 원인으로 사라지면 직전 값 유지`() {
        val headAtTop = feed(body() + (Joint.NOSE to Landmark(0.5f, 0.03f, 0.9f))) // (0.05 − 0.03) / 0.1
        assertEquals(0.2f, headAtTop.edge.top, 1e-4f)
        val glitch = feed(emptyMap())
        assertEquals(SOFTWARE_LOSS, glitch.state)
        assertEquals(EdgeWarning.NONE, glitch.edge)

        val walkingOut = feed(body(dx = 0.45f)).edge
        val gone = feed(emptyMap())
        assertEquals(USER_CAUSED_LOSS, gone.state)
        assertEquals(walkingOut, gone.edge)
    }

    @Test
    fun `화면 안 저신뢰 관절은 lowConfidence`() {
        val dim = Joint.RIGHT_WRIST to Landmark(0.63f, 0.53f, 0.2f)
        val one = feed(body() + dim)
        assertEquals(setOf(Joint.RIGHT_WRIST), one.lowConfidence)
        assertFalse(Joint.RIGHT_WRIST in one.landmarks)

        val all = feed(body(likelihood = 0.1f))
        assertEquals(SOFTWARE_LOSS, all.state)
        assertEquals(body().keys, all.lowConfidence)
    }

    @Test
    fun `시각이 뒤로 가면 사용자 원인`() {
        classifier.classify(body(), 10000)
        assertEquals(SOFTWARE_LOSS, classifier.classify(emptyMap(), 10033).state)
        assertEquals(USER_CAUSED_LOSS, classifier.classify(emptyMap(), 0).state)
    }
}
