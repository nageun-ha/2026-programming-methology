package com.example.prm_project.ui.exercise

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.prm_project.judge.ExerciseSession
import com.example.prm_project.judge.FakeExerciseSession
import com.example.prm_project.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/** 운동 화면 ViewModel. [ExerciseSession] 상태를 노출하고 종료 시 기록을 한 번만 만든다. */
class ExerciseViewModel(
    val chart: Chart,
    createSession: (CoroutineScope, Chart) -> ExerciseSession,
) : ViewModel() {

    private val session = createSession(viewModelScope, chart)
    val state: StateFlow<ExerciseState> = session.state
    val totalBeats: Int = chart.items.sumOf { it.beats }

    private val _record = MutableStateFlow<ExerciseRecord?>(null)
    val record: StateFlow<ExerciseRecord?> = _record.asStateFlow()

    init {
        viewModelScope.launch {
            state.first { it.phase == ExercisePhase.FINISHED }
            finish()
        }
    }

    fun start() = session.start()

    fun finish() {
        if (_record.value == null) _record.value = session.stop()
    }

    companion object {
        val FakeFactory = viewModelFactory {
            initializer {
                ExerciseViewModel(FakeExerciseSession.DEFAULT_CHART) { scope, chart ->
                    FakeExerciseSession(scope, chart = chart)
                }
            }
        }
    }
}
