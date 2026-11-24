package com.example.prm_project.judge

import com.example.prm_project.model.Joint
import com.example.prm_project.model.TargetPose
import kotlin.math.abs

/** 지연 오프셋 범위(ms). 임시값이고 첫 통합 데모(#36) 후 조정한다. */
const val MIN_LATENCY_OFFSET_MS = 0L
const val MAX_LATENCY_OFFSET_MS = 1000L

/** 박자 시각 앞뒤로 자세 완성을 찾는 구간(ms). 임시값이고 첫 통합 데모(#36) 후 조정한다. */
const val SEARCH_BEFORE_MS = 300L
const val SEARCH_AFTER_MS = 1500L

/** 완성 시각을 찾은 박자가 이보다 적으면 보정하지 않는다. 임시값이고 첫 통합 데모(#36) 후 조정한다. */
const val MIN_VALID_BEATS = 3

/** 프레임 하나를 [angles]로 잰 결과. [timestampMs]는 `PoseFrame.timestampMs`와 같다. */
data class AngleSample(val timestampMs: Long, val measured: Map<Joint, Float>)

/** 보정 화면에서 채보가 반짝이는 박자 하나와 그때 취할 자세. [beatTimeMs]는 운동 시작 기준이다. */
data class CalibrationBeat(val beatTimeMs: Long, val target: TargetPose)

/**
 * 박자 시각과 자세가 실제로 완성된 시각의 차이로 입력 지연 오프셋(ms)을 구한다.
 *
 * 모든 시각은 `SystemClock.elapsedRealtime()` 기준이다. 박자 시각은 `startedAtMs + beatTimeMs`다.
 * [samples]는 #47 위치 안내가 OK인 동안 측정한 프레임이어야 한다.
 * 판정 시점(`박자 시각 + 오프셋`)에 반영하는 것은 #26에서 한다.
 *
 * 1. 박자마다 박자 시각 − [SEARCH_BEFORE_MS] ~ + [SEARCH_AFTER_MS] 구간에서 처음 자세와 일치한 샘플 시각을 완성 시각으로 본다.
 *    목표 관절이 모두 측정되어 있고(NaN 아님) `target.toleranceDeg + toleranceOffsetDeg` 안이면 일치다.
 * 2. 구간 첫 샘플부터 일치하면(자세 유지 중) 언제 완성했는지 모르므로, 끝까지 일치하지 않으면 완성하지 않았으므로 그 박자는 뺀다.
 * 3. 남은 박자의 (완성 시각 − 박자 시각) 중앙값을 [MIN_LATENCY_OFFSET_MS]~[MAX_LATENCY_OFFSET_MS]로 자른다.
 *    일찍 완성하면 박자 시각에 이미 자세를 취하고 있으므로 판정 시점을 당기지 않는다(최소 0).
 *
 * @return 남은 박자가 [MIN_VALID_BEATS]보다 적으면 0(보정 안 함)
 */
fun latencyOffsetMs(
    startedAtMs: Long,
    beats: List<CalibrationBeat>,
    samples: List<AngleSample>,
    toleranceOffsetDeg: Float = 0f,
): Long {
    val sorted = samples.sortedBy { it.timestampMs }
    val delays = beats.mapNotNull { beat ->
        val beatAtMs = startedAtMs + beat.beatTimeMs
        val window = sorted.filter { it.timestampMs in beatAtMs - SEARCH_BEFORE_MS..beatAtMs + SEARCH_AFTER_MS }
        val first = window.indexOfFirst { it.matches(beat.target, toleranceOffsetDeg) }
        if (first <= 0) null else window[first].timestampMs - beatAtMs // -1: 일치 없음, 0: 자세 유지 중
    }
    if (delays.size < MIN_VALID_BEATS) return 0L
    return median(delays).coerceIn(MIN_LATENCY_OFFSET_MS, MAX_LATENCY_OFFSET_MS)
}

private fun AngleSample.matches(target: TargetPose, toleranceOffsetDeg: Float): Boolean {
    val allowed = target.toleranceDeg + toleranceOffsetDeg
    return target.targetAngles.all { (joint, goal) -> measured[joint]?.let { abs(it - goal) <= allowed } == true }
}

private fun median(values: List<Long>): Long {
    val sorted = values.sorted()
    val middle = sorted.size / 2
    return if (sorted.size % 2 == 1) sorted[middle] else (sorted[middle - 1] + sorted[middle]) / 2
}
