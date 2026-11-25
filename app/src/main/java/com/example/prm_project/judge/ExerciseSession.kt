package com.example.prm_project.judge

import com.example.prm_project.model.ExerciseRecord
import com.example.prm_project.model.ExerciseState
import kotlinx.coroutines.flow.StateFlow

/**
 * 운동 한 번을 진행하는 세션. 앱 안의 박자 시계는 세션만 가진다.
 *
 * 가짜 구현([FakeExerciseSession])과 실제 구현(#26)이 같은 모양이라 화면(③)은 바꿔 끼우기만 하면 된다.
 */
interface ExerciseSession {
    /** 지금 운동 상태. 시작 전에는 [ExerciseState.INITIAL]이다. */
    val state: StateFlow<ExerciseState>

    /** 운동을 시작한다. 박자 시계가 이때 돌기 시작한다. */
    fun start()

    /** 운동을 멈추고 지금까지의 기록을 돌려준다. 진행 중이든 끝났든 호출할 수 있다. */
    fun stop(): ExerciseRecord
}
