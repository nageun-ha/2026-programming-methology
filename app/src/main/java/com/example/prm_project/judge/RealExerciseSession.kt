package com.example.prm_project.judge

import android.os.SystemClock
import com.example.prm_project.model.CalibrationConfig
import com.example.prm_project.model.Chart
import com.example.prm_project.model.ChartItem
import com.example.prm_project.model.DetectionState
import com.example.prm_project.model.ExercisePhase
import com.example.prm_project.model.ExerciseRecord
import com.example.prm_project.model.ExerciseState
import com.example.prm_project.model.PoseFrame
import com.example.prm_project.model.TargetPose
import java.time.LocalDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 포즈 인식 프레임으로 박자마다 판정하는 실제 세션. 박자 진행은 [FakeExerciseSession]과 같다.
 *
 * - 자세 박자는 `startedAtMs + beatTimeMs + poseLatencyOffsetMs` 시각에, 그 시각 이하인 가장 최근 프레임으로 판정한다.
 * - 쓸 프레임이 없거나 판정 시각보다 [MAX_FRAME_AGE_MS] 넘게 오래됐으면(카메라·인식이 멈춤)
 *   [DetectionState.SOFTWARE_LOSS]와 같게 처리한다.
 * - 박자는 `startedAtMs + beatTimeMs` 시각에 맞춰 시작한다. 처리 시간이 다음 박자로 쌓이지 않는다.
 * - 휴식 박자에는 판정하지 않는다.
 *
 * @param frames 포즈 인식 프레임. PoseSource(#10)가 생기기 전이라 Flow를 직접 받는다.
 * @param poses 키는 [TargetPose.id]. 채보의 모든 poseId가 들어 있어야 한다.
 */
class RealExerciseSession(
    private val scope: CoroutineScope,
    private val frames: Flow<PoseFrame>,
    private val chart: Chart,
    private val poses: Map<String, TargetPose>,
    private val calibration: CalibrationConfig = CalibrationConfig(),
    private val clock: () -> Long = SystemClock::elapsedRealtime,
    private val today: () -> LocalDate = LocalDate::now,
) : ExerciseSession {

    private val _state = MutableStateFlow(ExerciseState.INITIAL)
    override val state: StateFlow<ExerciseState> = _state.asStateFlow()

    private val recentFrames = ArrayDeque<PoseFrame>()
    private var job: Job? = null
    private var judgedCount = 0
    private var successCount = 0
    private var softwareFailCount = 0

    override fun start() {
        if (job != null) return
        chart.items.filterIsInstance<ChartItem.Pose>().forEach {
            check(it.poseId in poses) { "채보의 poseId '${it.poseId}'가 자세 목록에 없다" }
        }
        val beatMs = 60_000L / chart.bpm
        val itemIndexOfBeat = chart.items.flatMapIndexed { index, item -> List(item.beats) { index } }
        val startedAtMs = clock()
        _state.value = ExerciseState.INITIAL.copy(phase = ExercisePhase.PLAYING, startedAtMs = startedAtMs)
        job = scope.launch {
            val collector = launch { frames.collect(::addFrame) }
            for ((beat, chartIndex) in itemIndexOfBeat.withIndex()) {
                delay(startedAtMs + beat * beatMs - clock())
                enterBeat(beat, beat * beatMs, chartIndex)
            }
            val end = itemIndexOfBeat.size
            delay(startedAtMs + end * beatMs - clock())
            collector.cancel()
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
            softwareFailCount = softwareFailCount,
        )
    }

    private fun addFrame(frame: PoseFrame) = synchronized(recentFrames) {
        recentFrames.addLast(frame)
        if (recentFrames.size > FRAME_BUFFER_SIZE) recentFrames.removeFirst()
    }

    private fun CoroutineScope.enterBeat(beat: Int, beatTimeMs: Long, chartIndex: Int) {
        val item = chart.items[chartIndex]
        val phase = if (item is ChartItem.Pose) ExercisePhase.PLAYING else ExercisePhase.REST
        _state.value = _state.value.copy(phase = phase, chartIndex = chartIndex, beatIndex = beat, beatTimeMs = beatTimeMs)
        if (item !is ChartItem.Pose) return
        launch {
            delay(calibration.poseLatencyOffsetMs)
            judgeBeat(poses.getValue(item.poseId), beat, beatTimeMs)
        }
    }

    private fun judgeBeat(pose: TargetPose, beat: Int, beatTimeMs: Long) {
        val judgeAtMs = _state.value.startedAtMs + beatTimeMs + calibration.poseLatencyOffsetMs
        val target = pose.copy(toleranceDeg = pose.toleranceDeg + calibration.toleranceOffsetDeg)
        val candidates = synchronized(recentFrames) {
            recentFrames.filter { it.timestampMs in judgeAtMs - MAX_FRAME_AGE_MS..judgeAtMs }.takeLast(JUDGE_FRAME_COUNT)
        }.ifEmpty { listOf(PoseFrame(emptyMap(), 1, 1, judgeAtMs, DetectionState.SOFTWARE_LOSS)) }
        val result = candidates.map { judge(it, target, beat, beatTimeMs) }.minBy { it.grade.ordinal }
        judgedCount++
        if (result.grade.isSuccess) successCount++
        if (result.recognitionFailed) softwareFailCount++
        _state.value = _state.value.copy(lastResult = result)
    }

    private companion object {
        /** 판정에 쓰는 최근 프레임 수. 여러 장이면 그중 가장 좋은 등급을 쓴다. */
        const val JUDGE_FRAME_COUNT = 1

        /** 들고 있는 최근 프레임 수. 30fps에서 약 2초. */
        const val FRAME_BUFFER_SIZE = 60

        /** 판정 시각보다 이만큼 넘게 오래된 프레임은 쓰지 않는다. 30fps 몇 프레임과 인식 지연을 넉넉히 덮는 값. */
        const val MAX_FRAME_AGE_MS = 200L
    }
}
