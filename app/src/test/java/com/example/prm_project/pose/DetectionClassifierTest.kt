package com.example.prm_project.pose

import com.example.prm_project.model.DetectionState.DETECTED
import com.example.prm_project.model.DetectionState.SOFTWARE_LOSS
import com.example.prm_project.model.DetectionState.USER_CAUSED_LOSS
import com.example.prm_project.model.Joint
import com.example.prm_project.model.Landmark
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DetectionClassifierTest {

    private val classifier = DetectionClassifier()
    private var t = 0L

    /** 30fps 한 프레임 넣기 */
    private fun feed(raw: Map<Joint, Landmark>) = classifier.classify(raw, t).also { t += 33 }

    /** 화면 가운데 선 사람을 dx만큼 옆으로 옮긴 관절 */
    private fun body(dx: Float = 0f, likelihood: Float = 0.9f) = mapOf(
        Joint.LEFT_SHOULDER to Landmark(0.40f + dx, 0.30f, likelihood),
        Joint.RIGHT_SHOULDER to Landmark(0.60f + dx, 0.30f, likelihood),
        Joint.RIGHT_WRIST to Landmark(0.63f + dx, 0.53f, likelihood),
        Joint.LEFT_ANKLE to Landmark(0.45f + dx, 0.88f, likelihood),
        Joint.RIGHT_ANKLE to Landmark(0.55f + dx, 0.88f, likelihood),
    )

    @Test
    fun `가장자리로 걸어 나가면 사용자 원인`() {
        assertEquals(DETECTED, feed(body()).second)
        assertEquals(DETECTED, feed(body(dx = 0.35f)).second) // 오른손목 x=0.98, 가장자리에 닿음

        val (partly, state) = feed(body(dx = 0.45f)) // 오른쪽 관절이 화면 밖
        assertEquals(USER_CAUSED_LOSS, state)
        assertTrue(partly.values.all { it.x <= 1f })

        val (gone, goneState) = feed(emptyMap()) // 완전히 나감
        assertEquals(USER_CAUSED_LOSS, goneState)
        assertTrue(gone.isEmpty())
    }

    @Test
    fun `가운데서 3프레임 끊기면 소프트웨어 원인이고 직전 좌표 유지`() {
        val last = feed(body()).first
        repeat(3) {
            val (landmarks, state) = feed(emptyMap())
            assertEquals(SOFTWARE_LOSS, state)
            assertEquals(last, landmarks)
        }
        assertEquals(DETECTED, feed(body()).second)
    }

    @Test
    fun `가운데서 오래 안 보이면 사용자 원인`() {
        feed(body())
        val states = List(40) { feed(emptyMap()).second } // 약 1.3초

        assertEquals(SOFTWARE_LOSS, states.first())
        assertEquals(USER_CAUSED_LOSS, states.last())
    }

    @Test
    fun `화면 안인데 신뢰도가 전부 낮으면 소프트웨어 원인`() {
        feed(body())
        assertEquals(SOFTWARE_LOSS, feed(body(likelihood = 0.1f)).second)
    }

    @Test
    fun `처음부터 아무도 없으면 사용자 원인`() {
        assertEquals(USER_CAUSED_LOSS, feed(emptyMap()).second)
    }
}
