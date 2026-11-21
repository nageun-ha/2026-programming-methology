package com.example.prm_project.judge

import com.example.prm_project.model.Joint
import com.example.prm_project.model.TargetPose

/** 모든 목표 자세의 허용 오차(°). 첫 통합 데모(#36) 뒤에 조정할 값이다. */
const val TARGET_POSE_TOLERANCE_DEG = 15f

/**
 * 처음 제시할 목표 자세 목록. 측정 방법과 값은 `documents/target-poses.md`에 있다.
 *
 * - 각도는 임시값, #25 머지 후 실측 중앙값으로 교체한다. 지금 값은 손으로 계산했다.
 * - [TargetPose.targetAngles]에는 그 자세를 정의하는 관절만 넣는다. 일치율은 넣은 관절의 평균이라,
 *   관절을 모두 넣으면 다른 자세와 구분이 흐려진다. 키는 [JOINT_ANGLES]의 키만 쓴다.
 * - id는 기록과 판정 결과가 참조하므로 바꾸거나 지우지 않는다. 새 자세는 뒤에 추가한다.
 */
val TARGET_POSES: List<TargetPose> = listOf(
    pose(
        "t_pose", "티 자세",
        Joint.LEFT_SHOULDER to 90f, Joint.RIGHT_SHOULDER to 90f,
        Joint.LEFT_ELBOW to 180f, Joint.RIGHT_ELBOW to 180f,
    ),
    pose(
        "y_pose", "만세",
        Joint.LEFT_SHOULDER to 150f, Joint.RIGHT_SHOULDER to 150f,
        Joint.LEFT_ELBOW to 180f, Joint.RIGHT_ELBOW to 180f,
    ),
    pose(
        "cactus_arms", "선인장 팔",
        Joint.LEFT_SHOULDER to 90f, Joint.RIGHT_SHOULDER to 90f,
        Joint.LEFT_ELBOW to 90f, Joint.RIGHT_ELBOW to 90f,
    ),
    pose(
        "left_arm_up", "왼팔 들기",
        Joint.LEFT_SHOULDER to 170f, Joint.RIGHT_SHOULDER to 15f,
        Joint.LEFT_ELBOW to 180f, Joint.RIGHT_ELBOW to 180f,
    ),
)

private fun pose(id: String, name: String, vararg angles: Pair<Joint, Float>) =
    TargetPose(id, name, mapOf(*angles), TARGET_POSE_TOLERANCE_DEG)
