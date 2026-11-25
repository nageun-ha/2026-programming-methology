package com.example.prm_project.model

/** 가이드 관절 위치. 좌표 규칙은 [Landmark]와 같고(0~1, 화면 기준), 화면 밖 값도 자르지 않는다. */
data class GuidePoint(val x: Float, val y: Float)

/**
 * 목표 자세를 사용자 체형에 맞춘 가이드 스켈레톤. ①이 사용자 영상 위에 겹쳐 그린다.
 *
 * @property points 관절 이름은 몸 기준이다. 계산할 수 없는 관절은 들어 있지 않다.
 */
data class GuidePose(val points: Map<Joint, GuidePoint>)
