package com.example.prm_project.model

/**
 * 판정에 쓰는 관절.
 *
 * 이름은 사용자 몸 기준이다. [LEFT_WRIST]는 화면 위치와 상관없이 실제 왼손목이다.
 */
enum class Joint {
    NOSE,
    LEFT_SHOULDER,
    RIGHT_SHOULDER,
    LEFT_ELBOW,
    RIGHT_ELBOW,
    LEFT_WRIST,
    RIGHT_WRIST,
    LEFT_HIP,
    RIGHT_HIP,
    LEFT_KNEE,
    RIGHT_KNEE,
    LEFT_ANKLE,
    RIGHT_ANKLE,
}
