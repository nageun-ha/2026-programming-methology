package com.example.prm_project.pose

import com.example.prm_project.model.DetectionState
import com.example.prm_project.model.Joint
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FakePoseSourceTest {

    /** 기기가 켜진 지 100초 된 시점에 수집을 시작한다고 가정한다. 시계는 가상 시간을 따라간다. */
    private fun TestScope.source() = FakePoseSource(clock = { START + testScheduler.currentTime })

    @Test
    fun `첫 3초 동안 프레임 90개`() = runTest {
        val frames = source().frames.takeWhile { it.timestampMs < START + 3000 }.toList()

        assertEquals(90, frames.size)
        assertEquals(START, frames.first().timestampMs) // elapsedRealtime 기준
        assertEquals(3000L, testScheduler.currentTime)
    }

    @Test
    fun `2~4초 사이 오른손목이 오른어깨보다 위`() = runTest {
        val raised = source().frames
            .takeWhile { it.timestampMs < START + 4000 }.toList()
            .filter { it.timestampMs >= START + 2000 }

        assertTrue(raised.isNotEmpty())
        raised.forEach {
            val wrist = it.landmarks.getValue(Joint.RIGHT_WRIST)
            val shoulder = it.landmarks.getValue(Joint.RIGHT_SHOULDER)
            assertTrue("t=${it.timestampMs}", wrist.y < shoulder.y)
        }
    }

    @Test
    fun `수집 쪽이 느려도 프레임 시각이 실제 시계보다 늦어지지 않는다`() = runTest {
        var lag = 0L
        source().frames
            .onEach { delay(5) } // 수집 쪽 처리 시간
            .take(300)
            .collect { lag = START + testScheduler.currentTime - it.timestampMs }

        assertTrue("lag=${lag}ms", lag < 50)
    }

    @Test
    fun `손실 구간 감지 상태와 반복`() {
        val source = FakePoseSource()
        assertEquals(DetectionState.USER_CAUSED_LOSS, source.frameAt(4500).detection)
        assertTrue(source.frameAt(4500).landmarks.isEmpty())
        assertEquals(DetectionState.SOFTWARE_LOSS, source.frameAt(5100).detection)
        assertEquals(DetectionState.DETECTED, source.frameAt(5300).detection)
    }

    private companion object {
        const val START = 100_000L
    }
}
