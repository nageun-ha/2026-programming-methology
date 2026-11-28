package com.example.prm_project.judge

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** 기본 기준값: 5프레임 연속이면 표시를 바꾸고, OK가 1500ms 이어지면 ready. 프레임 간격은 100ms. */
class FramingStabilizerTest {
    private val stabilizer = FramingStabilizer()
    private var nowMs = 0L

    /** [hint]를 [frames]프레임 넣고 마지막 결과를 돌려준다. */
    private fun feed(hint: FramingHint, frames: Int = 1): FramingStatus {
        var status: FramingStatus? = null
        repeat(frames) {
            status = stabilizer.update(hint, nowMs)
            nowMs += FRAME_MS
        }
        return status!!
    }

    @Test
    fun `OK와 COME_CLOSER가 번갈아 들어오면 표시가 바뀌지 않는다`() {
        repeat(10) {
            assertEquals(FramingHint.NOT_VISIBLE, feed(FramingHint.OK).hint)
            assertEquals(FramingHint.NOT_VISIBLE, feed(FramingHint.COME_CLOSER).hint)
        }
    }

    @Test
    fun `같은 안내가 5프레임 이어지면 바뀐다`() {
        assertEquals(FramingHint.NOT_VISIBLE, feed(FramingHint.OK, frames = 4).hint)
        assertEquals(FramingHint.OK, feed(FramingHint.OK).hint)
    }

    @Test
    fun `OK가 유지 시간 이상 이어져야 ready이고 다른 안내가 확정되면 다시 false`() {
        assertFalse(feed(FramingHint.OK, frames = 5).ready) // 400ms에 OK 확정
        assertFalse(feed(FramingHint.OK, frames = 14).ready) // 1800ms: 1400ms 유지
        assertTrue(feed(FramingHint.OK).ready) // 1900ms: 1500ms 유지

        assertTrue(feed(FramingHint.COME_CLOSER, frames = 4).ready) // 아직 확정 전이라 표시는 OK
        val changed = feed(FramingHint.COME_CLOSER)
        assertEquals(FramingHint.COME_CLOSER, changed.hint)
        assertFalse(changed.ready)

        assertFalse(feed(FramingHint.OK, frames = 5).ready) // 다시 OK가 돼도 시간은 처음부터
    }

    private companion object {
        const val FRAME_MS = 100L
    }
}
