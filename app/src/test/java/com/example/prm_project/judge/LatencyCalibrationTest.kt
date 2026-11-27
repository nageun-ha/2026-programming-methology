package com.example.prm_project.judge

import com.example.prm_project.model.Joint
import com.example.prm_project.model.TargetPose
import org.junit.Assert.assertEquals
import org.junit.Test

class LatencyCalibrationTest {

    private val pose = TargetPose("calib_raise_right", "오른손 들기", mapOf(Joint.RIGHT_SHOULDER to 170f, Joint.RIGHT_ELBOW to 180f), 15f)

    /** 2초마다 반짝이는 박자 [BEATS]개 */
    private val beats = List(BEATS) { CalibrationBeat(2000L * (it + 1), pose) }

    /**
     * [FRAME_MS] 간격 샘플. 박자 i 앞 400ms부터 다음 박자 앞 400ms까지는 박자 i의 구간이다.
     * [deviation]은 (박자 순번, 박자 기준 시각)에 목표에서 벗어난 각도를 준다. null이면 자세를 취하지 않은 것이다.
     */
    private fun samples(deviation: (Int, Long) -> Float?) = (0L..2000L * (BEATS + 1) step FRAME_MS).map { t ->
        val i = ((t + 400) / 2000 - 1).toInt()
        val off = if (i in 0 until BEATS) deviation(i, t - 2000L * (i + 1)) else null
        AngleSample(START + t, pose.targetAngles.mapValues { it.value - (off ?: 90f) })
    }

    /** 박자 i에서 박자 시각 + doneAfterMs[i]부터 목표 각도와 같다. null이면 그 박자는 완성하지 않는다. */
    private fun doneAt(doneAfterMs: List<Long?>) =
        samples { i, after -> doneAfterMs[i]?.let { if (after >= it) 0f else null } }

    private fun allDoneAt(ms: Long) = doneAt(List(BEATS) { ms })

    private fun offset(samples: List<AngleSample>, toleranceOffsetDeg: Float = 0f) =
        latencyOffsetMs(START, beats, samples, toleranceOffsetDeg)

    private fun assertNear(expected: Long, actual: Long) =
        assertEquals(expected.toFloat(), actual.toFloat(), FRAME_MS.toFloat())

    @Test
    fun `모든 박자에서 200ms 늦게 완성하면 약 200ms`() = assertNear(200, offset(allDoneAt(200)))

    @Test
    fun `한 박자만 1200ms 늦어도 약 200ms`() = assertNear(200, offset(doneAt(listOf(1200L) + List(BEATS - 1) { 200L })))

    @Test
    fun `박자보다 일찍 완성하면 0`() = assertEquals(MIN_LATENCY_OFFSET_MS, offset(allDoneAt(-200)))

    @Test
    fun `구간 안이지만 아주 늦으면 최대값으로 자른다`() = assertEquals(MAX_LATENCY_OFFSET_MS, offset(allDoneAt(1400)))

    @Test
    fun `구간 시작부터 자세를 유지 중인 박자는 뺀다`() {
        // 넣으면 중앙값이 (−300 + 600) / 2 = 150 근처가 된다
        assertNear(600, offset(doneAt(List(3) { -400L } + List(3) { 600L } + null)))
    }

    @Test
    fun `끝까지 일치하지 않는 박자는 뺀다`() = assertNear(600, offset(doneAt(List(4) { null } + List(3) { 600L })))

    @Test
    fun `유효 박자가 MIN_VALID_BEATS보다 적으면 0`() =
        assertEquals(0L, offset(doneAt(List(BEATS - MIN_VALID_BEATS + 1) { null } + List(MIN_VALID_BEATS - 1) { 200L })))

    @Test
    fun `목표 관절이 없거나 NaN이면 일치로 보지 않는다`() {
        val missing = allDoneAt(200).map { it.copy(measured = it.measured - Joint.RIGHT_ELBOW) }
        val nan = allDoneAt(200).map { it.copy(measured = it.measured + (Joint.RIGHT_ELBOW to Float.NaN)) }

        assertEquals(0L, offset(missing))
        assertEquals(0L, offset(nan))
    }

    @Test
    fun `허용 오차 보정치를 주면 그만큼 일찍 일치로 본다`() {
        // 100ms에 20° 차이까지 다가오고 400ms에 완성
        val approaching = samples { _, after -> if (after >= 400) 0f else if (after >= 100) 20f else null }

        assertNear(400, offset(approaching))
        assertNear(100, offset(approaching, toleranceOffsetDeg = 10f))
    }

    private companion object {
        const val START = 10_000L
        const val BEATS = 7
        const val FRAME_MS = 33L
    }
}
