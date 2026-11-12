package com.example.prm_project.model

/** 한 프레임의 포즈 인식 상태. */
enum class DetectionState {
    /** 정상 인식. 관절 위치로 판정한다. */
    DETECTED,

    /** 사용자 원인 인식 실패(카메라 이탈 등). [Grade.MISS]로 판정한다. */
    USER_CAUSED_LOSS,

    /**
     * 소프트웨어 원인 인식 실패.
     * [Grade.GREAT]로 표시하고 성공으로 세며, [JudgeResult.recognitionFailed]를 true로 둔다.
     */
    SOFTWARE_LOSS,
}
