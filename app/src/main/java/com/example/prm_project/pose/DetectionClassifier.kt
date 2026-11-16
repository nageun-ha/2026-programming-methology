package com.example.prm_project.pose

import com.example.prm_project.model.DetectionState
import com.example.prm_project.model.Joint
import com.example.prm_project.model.Landmark
import com.example.prm_project.pose.DetectionThresholds.EDGE_MARGIN
import com.example.prm_project.pose.DetectionThresholds.MIN_LIKELIHOOD
import com.example.prm_project.pose.DetectionThresholds.SOFTWARE_LOSS_MAX_MS

/** 감지 상태 판단 기준값. 첫 통합 데모(#36) 후 실기기에서 조정한다. */
object DetectionThresholds {
    /** 신뢰도가 이 값 미만인 관절은 잡히지 않은 것으로 본다. */
    const val MIN_LIKELIHOOD = 0.5f

    /** 관절이 화면 가장자리에서 이 비율 안쪽에 있으면 "가장자리에 닿았다"고 본다. */
    const val EDGE_MARGIN = 0.05f

    /** 가운데서 사라진 뒤 이 시간까지는 소프트웨어 원인으로 보고 직전 좌표를 유지한다. 넘으면 사용자 원인이다. */
    const val SOFTWARE_LOSS_MAX_MS = 1000L
}

/**
 * 관절 위치와 신뢰도로 [DetectionState]를 정하고 [com.example.prm_project.model.PoseFrame]에 넣을 관절을 고른다.
 *
 * 입력은 정규화·회전·거울 반전을 마친 라이브러리 출력이다. 화면 밖으로 추정된 관절은 좌표가 0~1을 벗어날 수 있다.
 * 출력 관절은 화면 안(0~1)에 있고 신뢰도가 [MIN_LIKELIHOOD] 이상인 것만 남긴다.
 * 직전 프레임을 기억하므로 한 카메라 흐름에 인스턴스 하나를 두고 프레임 순서대로 넣는다.
 *
 * | 상황 | 상태 | 내보내는 관절 |
 * |---|---|---|
 * | 잡힌 관절이 모두 화면 안 | DETECTED | 잡힌 관절 |
 * | 일부 관절 위치가 화면 밖 (전신이 안 나옴) | USER_CAUSED_LOSS | 화면 안 관절 |
 * | 잡힌 관절 없음, 직전에 가장자리에 닿아 있었음 | USER_CAUSED_LOSS | 없음 |
 * | 잡힌 관절 없음, 가운데서 사라진 지 [SOFTWARE_LOSS_MAX_MS] 이내 | SOFTWARE_LOSS | 직전 관절 (깜빡임 방지) |
 * | 잡힌 관절 없음, 그 시간을 넘김 | USER_CAUSED_LOSS | 없음 |
 */
class DetectionClassifier {
    private var lastSeen: Map<Joint, Landmark> = emptyMap()
    private var lastSeenAtEdge = false
    private var lostSinceMs: Long? = null

    fun classify(raw: Map<Joint, Landmark>, timestampMs: Long): Pair<Map<Joint, Landmark>, DetectionState> {
        val confident = raw.filterValues { it.likelihood >= MIN_LIKELIHOOD }
        if (confident.isEmpty()) return lost(timestampMs)

        lostSinceMs = null
        val inside = confident.filterValues { it.isInside() }
        val partlyOut = raw.values.any { !it.isInside() } // 화면 밖 관절은 신뢰도가 낮게 나오므로 raw로 본다
        lastSeen = inside
        lastSeenAtEdge = partlyOut || inside.values.any { it.nearEdge() }
        return inside to if (partlyOut) DetectionState.USER_CAUSED_LOSS else DetectionState.DETECTED
    }

    private fun lost(timestampMs: Long): Pair<Map<Joint, Landmark>, DetectionState> {
        val since = lostSinceMs ?: timestampMs.also { lostSinceMs = it }
        val softwareGlitch = lastSeen.isNotEmpty() && !lastSeenAtEdge && timestampMs - since <= SOFTWARE_LOSS_MAX_MS
        return if (softwareGlitch) lastSeen to DetectionState.SOFTWARE_LOSS
        else emptyMap<Joint, Landmark>() to DetectionState.USER_CAUSED_LOSS
    }

    private fun Landmark.isInside() = x in 0f..1f && y in 0f..1f

    private fun Landmark.nearEdge() =
        x < EDGE_MARGIN || x > 1 - EDGE_MARGIN || y < EDGE_MARGIN || y > 1 - EDGE_MARGIN
}
