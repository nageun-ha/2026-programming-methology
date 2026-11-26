package com.example.prm_project.pose

import com.example.prm_project.model.PoseFrame
import kotlinx.coroutines.flow.Flow

/** 포즈 프레임 공급원. 가짜([FakePoseSource])와 카메라 구현을 같은 자리에 바꿔 끼운다. */
interface PoseSource {
    val frames: Flow<PoseFrame>
}
