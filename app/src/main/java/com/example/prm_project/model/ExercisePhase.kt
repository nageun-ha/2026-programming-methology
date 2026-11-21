package com.example.prm_project.model

/** 운동 진행 단계. */
enum class ExercisePhase {
    /** 시작 전 준비. */
    READY,

    /** 자세 구간 진행 중. */
    PLAYING,

    /** 휴식 구간. */
    REST,

    /** 채보가 끝남. */
    FINISHED,
}
