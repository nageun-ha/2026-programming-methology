package com.example.prm_project.model

/**
 * 운동 한 번의 채보.
 *
 * @property bpm 분당 박자 수
 * @property items 순서대로 진행할 자세와 휴식
 */
data class Chart(val bpm: Int, val items: List<ChartItem>)

/**
 * 채보 한 칸. 자세 또는 휴식이다.
 *
 * @property beats 길이(박자 수)
 */
sealed interface ChartItem {
    val beats: Int

    /** 따라 할 자세. [poseId]는 [TargetPose.id]와 같은 값이다. */
    data class Pose(val poseId: String, override val beats: Int) : ChartItem

    /** 휴식. 판정하지 않는다. */
    data class Rest(override val beats: Int) : ChartItem
}
