package com.example.prm_project.pose

import com.example.prm_project.model.PoseFrame
import kotlinx.coroutines.flow.Flow

/**
 * 포즈 프레임 공급원. 가짜([FakePoseSource])와 카메라([CameraPoseSource]) 구현을 같은 자리에 바꿔 끼운다.
 *
 * 화면과 판정 세션이 같은 프레임을 써야 한다. [CameraPoseSource.frames]는 이미 공유되는 흐름이라 여러 곳에서 수집해도
 * 카메라는 한 번만 연결된다. [FakePoseSource.frames]는 수집할 때마다 시나리오가 처음부터 시작하므로
 * ViewModel에서 `shareIn(viewModelScope, SharingStarted.WhileSubscribed())`로 한 번 감싸 두 곳에 넘긴다.
 */
interface PoseSource {
    val frames: Flow<PoseFrame>
}
