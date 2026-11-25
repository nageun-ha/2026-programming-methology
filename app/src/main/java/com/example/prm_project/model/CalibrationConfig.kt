package com.example.prm_project.model

/** 운동 판정에 적용할 로컬 보정값. 보정 전에는 각도와 시각에 오프셋을 더하지 않는다. */
data class CalibrationConfig(
    val toleranceOffsetDeg: Float = 0f,
    val poseLatencyOffsetMs: Long = 0L,
)
