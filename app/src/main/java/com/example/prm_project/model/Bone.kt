package com.example.prm_project.model

/**
 * 가이드 스켈레톤의 뼈. 부모 뼈가 자식 뼈보다 앞에 온다.
 *
 * - 양 끝은 관절 목록이다. 둘이면 그 중점이다(몸통과 목).
 * - 뼈 방향은 픽셀 좌표에서 [from]→[to]로 잰 화면 기준 각도(°)다. 0° = 화면 오른쪽(+x), 90° = 화면 아래(+y).
 *   `atan2(dy, dx)`와 같다. 정규화 좌표에서 재면 비정사각 이미지에서 틀어지므로 반드시 픽셀로 바꿔 잰다.
 */
enum class Bone(val from: List<Joint>, val to: List<Joint>) {
    TORSO(listOf(Joint.LEFT_HIP, Joint.RIGHT_HIP), listOf(Joint.LEFT_SHOULDER, Joint.RIGHT_SHOULDER)),
    NECK(listOf(Joint.LEFT_SHOULDER, Joint.RIGHT_SHOULDER), listOf(Joint.NOSE)),
    LEFT_UPPER_ARM(listOf(Joint.LEFT_SHOULDER), listOf(Joint.LEFT_ELBOW)),
    LEFT_FOREARM(listOf(Joint.LEFT_ELBOW), listOf(Joint.LEFT_WRIST)),
    RIGHT_UPPER_ARM(listOf(Joint.RIGHT_SHOULDER), listOf(Joint.RIGHT_ELBOW)),
    RIGHT_FOREARM(listOf(Joint.RIGHT_ELBOW), listOf(Joint.RIGHT_WRIST)),
    LEFT_THIGH(listOf(Joint.LEFT_HIP), listOf(Joint.LEFT_KNEE)),
    LEFT_SHIN(listOf(Joint.LEFT_KNEE), listOf(Joint.LEFT_ANKLE)),
    RIGHT_THIGH(listOf(Joint.RIGHT_HIP), listOf(Joint.RIGHT_KNEE)),
    RIGHT_SHIN(listOf(Joint.RIGHT_KNEE), listOf(Joint.RIGHT_ANKLE)),
}
