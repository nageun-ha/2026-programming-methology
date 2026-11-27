package com.example.prm_project.ui.result

import com.example.prm_project.data.RecordRepository
import com.example.prm_project.model.ExerciseRecord
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** 운동 한 번에 하나씩 만들어 완료 콜백에서 호출한다. 저장 실패 시 같은 기록으로 재시도한다. */
class ExerciseResultRecorder(private val repository: RecordRepository) {
    private val mutex = Mutex()
    private var saved = false

    suspend fun save(record: ExerciseRecord) = mutex.withLock {
        if (saved) return@withLock

        // 저장 중 화면을 떠나도 저장과 중복 방지 상태 갱신은 함께 끝낸다.
        withContext(NonCancellable) {
            repository.save(record)
            saved = true
        }
    }
}
