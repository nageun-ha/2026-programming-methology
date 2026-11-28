package com.example.prm_project.judge

/**
 * 보정·운동 시작 전에 사용자에게 보여 줄 위치 안내. ③(#34, #35)이 문구로 표시한다.
 *
 * 좌우는 화면(거울) 기준이다. [com.example.prm_project.model.PoseFrame] 좌표가 이미 화면 기준이고,
 * 거울 미리보기에서는 화면 왼쪽 = 사용자 자신의 왼쪽이라 사용자가 느끼는 방향과도 같다.
 */
enum class FramingHint {
    /** 제대로 잡혔다. */
    OK,

    /** 미리보기 화면에서 왼쪽으로 이동. 아직 [judgeFraming]이 내보내지 않음, outOfFrame 추가 후 구현(#47). */
    MOVE_LEFT,

    /** 미리보기 화면에서 오른쪽으로 이동. 아직 [judgeFraming]이 내보내지 않음, outOfFrame 추가 후 구현(#47). */
    MOVE_RIGHT,

    /** 머리나 발이 잘렸으니 뒤로 물러남. 아직 [judgeFraming]이 내보내지 않음, outOfFrame 추가 후 구현(#47). */
    STEP_BACK,

    /** 화면 대비 몸이 너무 작으니 가까이 옴. */
    COME_CLOSER,

    /** 관절이 하나도 잡히지 않음. */
    NOT_VISIBLE,

    /** 관절은 있지만 신뢰도가 전부 낮음. 조명이 어두울 때다. */
    LOW_LIGHT,
}
