package com.example.prm_project.model

/**
 * 판정(②)이 화면(③)에 내보내는 운동 상태.
 *
 * - 박자 시계는 ②(판정, ExerciseSession)만 가진다.
 * - 모든 시각은 `SystemClock.elapsedRealtime()` 기준(ms)이다. [PoseFrame.timestampMs]도 같은 기준이어야 한다.
 * - ③(화면)은 자체 타이머를 만들지 않는다. 현재 위치는 `elapsedRealtime() - startedAtMs`로 계산하고
 *   [beatTimeMs]와 비교해 박자 사이를 보간한다.
 *
 * @property chartIndex 지금 진행 중인 [Chart.items]의 순번
 * @property beatIndex 운동 시작부터 센 박자 순번
 * @property startedAtMs 운동을 시작한 시각(elapsedRealtime 기준). 시작 전([ExercisePhase.READY])에는 0이다.
 * @property beatTimeMs 운동 시작 기준으로 이번 박자의 시각(ms)
 * @property lastResult 가장 최근 판정. 아직 없으면 null이다.
 */
data class ExerciseState(
    val phase: ExercisePhase,
    val chartIndex: Int,
    val beatIndex: Int,
    val startedAtMs: Long,
    val beatTimeMs: Long,
    val lastResult: JudgeResult?,
) {
    companion object {
        /** 운동을 시작하기 전의 상태. */
        val INITIAL = ExerciseState(ExercisePhase.READY, 0, 0, 0L, 0L, null)
    }
}
