package com.example.prm_project.judge

import com.example.prm_project.model.ExercisePhase
import com.example.prm_project.model.Grade
import com.example.prm_project.model.JudgeResult
import java.time.LocalDate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/** 기본 채보: fake_a 3박(0~2) → fake_b 3박(3~5) → 휴식 2박(6~7) → fake_c 3박(8~10). 1박 = 1000ms. */
@OptIn(ExperimentalCoroutinesApi::class)
class FakeExerciseSessionTest {

    private fun TestScope.startedSession() =
        FakeExerciseSession(backgroundScope, clock = { START_MS }, today = { TODAY }).also {
            it.start()
            runCurrent()
        }

    private fun TestScope.advanceBeats(beats: Int) {
        advanceTimeBy(beats * BEAT_MS)
        runCurrent()
    }

    /** 채보가 끝날 때까지 진행하며 박자마다 새로 나온 판정을 모은다. */
    private fun TestScope.playToEnd(session: FakeExerciseSession): List<JudgeResult> {
        val results = mutableListOf<JudgeResult>()
        while (session.state.value.phase != ExercisePhase.FINISHED) {
            val state = session.state.value
            state.lastResult?.takeIf { it.beatIndex == state.beatIndex }?.let(results::add)
            advanceBeats(1)
        }
        return results
    }

    @Test
    fun `10박자 진행하면 beatIndex가 10`() = runTest {
        val session = startedSession()

        advanceBeats(10)

        assertEquals(10, session.state.value.beatIndex)
        assertEquals(10 * BEAT_MS, session.state.value.beatTimeMs)
    }

    @Test
    fun `휴식 구간 박자에서는 phase가 REST이고 판정하지 않는다`() = runTest {
        val session = startedSession()

        advanceBeats(6)
        assertEquals(ExercisePhase.REST, session.state.value.phase)
        advanceBeats(1)
        assertEquals(ExercisePhase.REST, session.state.value.phase)
        assertEquals(5, session.state.value.lastResult?.beatIndex)
        advanceBeats(1)
        assertEquals(ExercisePhase.PLAYING, session.state.value.phase)
    }

    @Test
    fun `등급은 PERFECT GREAT BAD MISS 순서로 돌고 휴식 뒤에도 이어진다`() = runTest {
        val grades = playToEnd(startedSession()).map { it.grade }

        val cycle = listOf(Grade.PERFECT, Grade.GREAT, Grade.BAD, Grade.MISS)
        assertEquals(List(9) { cycle[it % 4] }, grades)
    }

    @Test
    fun `stop의 successCount는 나온 등급의 isSuccess 개수와 같다`() = runTest {
        val session = startedSession()
        val results = playToEnd(session)

        val record = session.stop()

        assertEquals(results.size, record.judgedCount)
        assertEquals(results.count { it.grade.isSuccess }, record.successCount)
        assertEquals(11, record.durationSec)
        assertEquals(TODAY, record.date)
    }

    @Test
    fun `startedAtMs는 주입한 시계 값과 같다`() = runTest {
        assertEquals(START_MS, startedSession().state.value.startedAtMs)
    }

    private companion object {
        const val BEAT_MS = 1_000L
        const val START_MS = 123_456L
        val TODAY: LocalDate = LocalDate.of(2026, 11, 21)
    }
}
