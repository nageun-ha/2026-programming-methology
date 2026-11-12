package com.example.prm_project.model

/**
 * 박자 하나에 대한 판정 결과. 일치율은 넣지 않는다.
 *
 * @property recognitionFailed 소프트웨어 원인 인식 실패면 true다. 이때 [grade]는 [Grade.GREAT]이고 성공으로 센다.
 * @property wrongJoints 틀린 관절. 알려 줄 것이 없으면 null이다.
 */
data class JudgeResult(
    val poseId: String,
    val grade: Grade,
    val recognitionFailed: Boolean,
    val beatIndex: Int,
    val elapsedMs: Long,
    val wrongJoints: List<Joint>? = null,
)
