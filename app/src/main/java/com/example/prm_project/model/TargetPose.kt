package com.example.prm_project.model

import androidx.annotation.DrawableRes

/**
 * 따라 해야 할 목표 자세.
 *
 * @property id 영어 문자열. 기록과 판정 결과가 참조하므로 한번 정하면 바꾸지 않는다.
 * @property targetAngles 키는 각도의 꼭짓점 관절, 값은 목표 각도(0~180°)다.
 * @property toleranceDeg 허용 오차(°)
 * @property chartImageRes 채보 그림. 없으면 null이다.
 */
data class TargetPose(
    val id: String,
    val name: String,
    val targetAngles: Map<Joint, Float>,
    val toleranceDeg: Float,
    @DrawableRes val chartImageRes: Int? = null,
)
