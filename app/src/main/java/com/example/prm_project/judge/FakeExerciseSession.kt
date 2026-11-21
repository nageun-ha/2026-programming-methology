package com.example.prm_project.judge

import android.os.SystemClock
import com.example.prm_project.model.Chart
import com.example.prm_project.model.ChartItem
import com.example.prm_project.model.ExercisePhase
import com.example.prm_project.model.ExerciseRecord
import com.example.prm_project.model.ExerciseState
import com.example.prm_project.model.Grade
import com.example.prm_project.model.JudgeResult
import java.time.LocalDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 실제 판정 없이 정해진 순서로 판정 결과를 흘려보내는 가짜 세션. ③·④가 화면을 만들 때 쓴다.
 *
 * - 박자는 시작하는 순간(beatIndex 0)부터 센다. 마지막 박자가 끝나면 [ExercisePhase.FINISHED]가 된다.
 * - 자세 박자마다 등급을 PERFECT → GREAT → BAD → MISS 순서로 돌려 [ExerciseState.lastResult]에 넣는다.
 * - 휴식 박자에는 판정하지 않고, 등급 순서도 넘기지 않는다.
 * - 성공 여부는 [Grade.isSuccess]만 따른다.
 *
 * @param scope 박자 코루틴을 띄울 곳
 * @param clock [ExerciseState.startedAtMs]에 쓰는 시계(elapsedRealtime 기준)
 * @param today [ExerciseRecord.date]에 쓰는 날짜
 */
class FakeExerciseSession(
    private val scope: CoroutineScope,
    private val clock: () -> Long = SystemClock::elapsedRealtime,
    private val today: () -> LocalDate = LocalDate::now,
    private val chart: Chart = DEFAULT_CHART,
) : ExerciseSession {

    private val _state = MutableStateFlow(ExerciseState.INITIAL)
    override val state: StateFlow<ExerciseState> = _state.asStateFlow()

    private var job: Job? = null
    private var judgedCount = 0
    private var successCount = 0

    override fun start() {
        if (job != null) return
        val beatMs = 60_000L / chart.bpm
        val itemIndexOfBeat = chart.items.flatMapIndexed { index, item -> List(item.beats) { index } }
        _state.value = ExerciseState.INITIAL.copy(phase = ExercisePhase.PLAYING, startedAtMs = clock())
        job = scope.launch {
            for ((beat, chartIndex) in itemIndexOfBeat.withIndex()) {
                if (beat > 0) delay(beatMs)
                enterBeat(beat, beat * beatMs, chartIndex)
            }
            delay(beatMs)
            val end = itemIndexOfBeat.size
            _state.value = _state.value.copy(phase = ExercisePhase.FINISHED, beatIndex = end, beatTimeMs = end * beatMs)
        }
    }

    override fun stop(): ExerciseRecord {
        job?.cancel()
        return ExerciseRecord(
            date = today(),
            durationSec = (_state.value.beatTimeMs / 1000).toInt(),
            judgedCount = judgedCount,
            successCount = successCount,
            softwareFailCount = 0,
        )
    }

    private fun enterBeat(beat: Int, beatTimeMs: Long, chartIndex: Int) {
        val base = _state.value.copy(chartIndex = chartIndex, beatIndex = beat, beatTimeMs = beatTimeMs)
        val item = chart.items[chartIndex]
        _state.value = if (item is ChartItem.Pose) {
            base.copy(phase = ExercisePhase.PLAYING, lastResult = nextResult(item.poseId, beat, beatTimeMs))
        } else {
            base.copy(phase = ExercisePhase.REST)
        }
    }

    private fun nextResult(poseId: String, beat: Int, beatTimeMs: Long): JudgeResult {
        val grade = GRADE_CYCLE[judgedCount % GRADE_CYCLE.size]
        judgedCount++
        if (grade.isSuccess) successCount++
        return JudgeResult(poseId, grade, recognitionFailed = false, beatIndex = beat, elapsedMs = beatTimeMs)
    }

    companion object {
        /** BPM 60, 자세 3개 + 휴식 1회, 11박자. poseId는 #20에서 정해지기 전이라 가짜임을 드러낸다. */
        val DEFAULT_CHART = Chart(
            bpm = 60,
            items = listOf(
                ChartItem.Pose("fake_a", 3),
                ChartItem.Pose("fake_b", 3),
                ChartItem.Rest(2),
                ChartItem.Pose("fake_c", 3),
            ),
        )

        private val GRADE_CYCLE = listOf(Grade.PERFECT, Grade.GREAT, Grade.BAD, Grade.MISS)
    }
}
