package com.example.prm_project.judge

import com.example.prm_project.model.CalibrationConfig
import com.example.prm_project.model.Chart
import com.example.prm_project.model.ChartItem
import com.example.prm_project.model.DetectionState
import com.example.prm_project.model.ExercisePhase
import com.example.prm_project.model.ExerciseRecord
import com.example.prm_project.model.Grade
import com.example.prm_project.model.JudgeResult
import com.example.prm_project.model.PoseFrame
import com.example.prm_project.model.TargetPose
import java.time.LocalDate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/** BPM 60이라 1박 = 1000ms. 프레임은 인식 상태만으로 등급이 정해지게 만든다. */
@OptIn(ExperimentalCoroutinesApi::class)
class RealExerciseSessionTest {

    private val frames = MutableSharedFlow<PoseFrame>(extraBufferCapacity = 64)

    private fun TestScope.startedSession(
        chart: Chart,
        offsetMs: Long = 0L,
        clock: () -> Long = { testScheduler.currentTime },
    ) = RealExerciseSession(
        scope = backgroundScope,
        frames = frames,
        chart = chart,
        poses = mapOf("a" to TargetPose("a", "A", emptyMap(), 10f)),
        calibration = CalibrationConfig(poseLatencyOffsetMs = offsetMs),
        clock = clock,
        today = { TODAY },
    ).also {
        it.start()
        runCurrent()
    }

    /** 끝날 때까지 1박씩 진행하며 새 판정을 모은다. 박자마다 [onBeat]를 부른다. */
    private fun TestScope.playToEnd(session: RealExerciseSession, onBeat: () -> Unit = {}): List<JudgeResult> {
        val results = mutableListOf<JudgeResult>()
        while (session.state.value.phase != ExercisePhase.FINISHED) {
            val state = session.state.value
            assertEquals(testScheduler.currentTime, state.startedAtMs + state.beatTimeMs)
            state.lastResult?.takeIf { it.beatIndex == state.beatIndex }?.let(results::add)
            onBeat()
            advanceTimeBy(BEAT_MS)
            runCurrent()
        }
        return results
    }

    private fun emit(detection: DetectionState, timestampMs: Long) =
        frames.tryEmit(PoseFrame(emptyMap(), 1, 1, timestampMs, detection))

    @Test
    fun `자세 10박자면 판정 10번, 프레임이 없으면 전부 인식 실패로 성공`() = runTest {
        advanceTimeBy(500)
        val session = startedSession(Chart(60, listOf(ChartItem.Pose("a", 10))))

        val results = playToEnd(session)
        val record = session.stop()

        assertEquals((0..9).toList(), results.map { it.beatIndex })
        assertTrue(results.all { it.recognitionFailed && it.grade == Grade.GREAT })
        assertEquals(
            ExerciseRecord(TODAY, 10, 10, 10, 10, gradeCounts = mapOf(
                Grade.PERFECT to 0, Grade.GREAT to 10, Grade.BAD to 0, Grade.MISS to 0,
            )),
            record,
        )
    }

    @Test
    fun `휴식 박자에는 판정하지 않는다`() = runTest {
        val chart = Chart(60, listOf(ChartItem.Pose("a", 2), ChartItem.Rest(2), ChartItem.Pose("a", 2)))
        val session = startedSession(chart)

        val results = playToEnd(session)

        assertEquals(listOf(0, 1, 4, 5), results.map { it.beatIndex })
        val record = session.stop()
        assertEquals(4, record.judgedCount)
        assertEquals(4, record.gradeCounts.getValue(Grade.GREAT))
    }

    @Test
    fun `stop의 성공 횟수는 판정 결과의 성공 수와 같다`() = runTest {
        val session = startedSession(Chart(60, listOf(ChartItem.Pose("a", 6))))
        var beat = 0
        val results = playToEnd(session) {
            val detection = if (beat++ % 3 == 0) DetectionState.SOFTWARE_LOSS else DetectionState.USER_CAUSED_LOSS
            emit(detection, testScheduler.currentTime + BEAT_MS)
        }

        val record = session.stop()

        assertEquals(Grade.entries.associateWith { grade -> results.count { it.grade == grade } }, record.gradeCounts)
        assertEquals(record.judgedCount, record.gradeCounts.values.sum())
        assertEquals(results.count { it.grade.isSuccess }, record.successCount)
        assertEquals(results.count { it.recognitionFailed }, record.softwareFailCount)
        assertEquals(listOf(true, true, false, false, true, false), results.map { it.grade.isSuccess })
    }

    @Test
    fun `새 세션은 모든 등급을 0회부터 센다`() = runTest {
        val chart = Chart(60, listOf(ChartItem.Rest(1), ChartItem.Pose("a", 1)))
        val first = startedSession(chart)
        playToEnd(first)
        assertEquals(1, first.stop().gradeCounts.getValue(Grade.GREAT))

        val second = startedSession(chart)
        val record = second.stop()
        assertEquals(0, record.judgedCount)
        assertEquals(Grade.entries.associateWith { 0 }, record.gradeCounts)
    }

    @Test
    fun `poseLatencyOffsetMs만큼 늦은 프레임으로 판정한다`() {
        assertEquals(Grade.MISS, beat1Result(offsetMs = 0).grade)
        assertTrue(beat1Result(offsetMs = 200).recognitionFailed)
    }

    /** 1박 시각에 사용자 원인 실패, 200ms 뒤에 소프트웨어 원인 실패 프레임을 두고 1박 판정을 본다. */
    private fun beat1Result(offsetMs: Long): JudgeResult {
        lateinit var result: JudgeResult
        runTest {
            val session = startedSession(Chart(60, listOf(ChartItem.Pose("a", 2))), offsetMs)
            emit(DetectionState.USER_CAUSED_LOSS, BEAT_MS)
            emit(DetectionState.SOFTWARE_LOSS, BEAT_MS + 200)
            advanceTimeBy(BEAT_MS + offsetMs)
            runCurrent()
            result = session.state.value.lastResult!!
        }
        assertEquals(1, result.beatIndex)
        return result
    }

    @Test
    fun `판정 시각보다 오래된 프레임만 있으면 인식 실패로 본다`() = runTest {
        val session = startedSession(Chart(60, listOf(ChartItem.Pose("a", 2))))
        emit(DetectionState.USER_CAUSED_LOSS, 0) // 1박 판정 시각보다 1000ms 전, 그 뒤로 카메라가 멈춤

        advanceTimeBy(BEAT_MS)
        runCurrent()

        val result = session.state.value.lastResult!!
        assertEquals(1, result.beatIndex)
        assertTrue(result.recognitionFailed) // 오래된 프레임으로 판정하면 MISS
    }

    @Test
    fun `처리가 늦어져도 다음 박자는 목표 시각에 시작한다`() = runTest {
        var lagMs = 0L // 실제 시계가 가상 시간보다 앞선 만큼. 처리 시간이 걸린 것처럼 흉내 낸다
        val session = startedSession(Chart(60, listOf(ChartItem.Pose("a", 3))), clock = { testScheduler.currentTime + lagMs })
        lagMs = 300

        advanceTimeBy(BEAT_MS) // 1박 진입. 이때 시계는 1300이라 2박까지 700ms만 기다려야 한다
        runCurrent()
        advanceTimeBy(BEAT_MS - lagMs)
        runCurrent()

        assertEquals(2, session.state.value.beatIndex) // delay(beatMs)를 반복하면 아직 1박
    }

    @Test
    fun `자세 목록에 없는 poseId가 있으면 시작할 때 예외`() = runTest {
        val session = RealExerciseSession(backgroundScope, frames, Chart(60, listOf(ChartItem.Pose("x", 1))), emptyMap())

        assertThrows(IllegalStateException::class.java) { session.start() }
    }

    private companion object {
        const val BEAT_MS = 1_000L
        val TODAY: LocalDate = LocalDate.of(2026, 11, 25)
    }
}
