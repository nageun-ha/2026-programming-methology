package com.example.prm_project.judge

/**
 * 화면에 표시할 위치 안내.
 *
 * @property hint 깜빡임 방지를 거친 안내
 * @property ready [FramingHint.OK]가 충분히 이어져 준비 완료인지
 */
data class FramingStatus(val hint: FramingHint, val ready: Boolean)

/**
 * 프레임마다 바뀌는 [judgeFraming] 결과를 안정된 표시값으로 바꾼다.
 *
 * - 같은 안내가 연속 [FramingThresholds.stableFrames]프레임 들어와야 표시를 바꾼다. 첫 프레임 전에는 [FramingHint.NOT_VISIBLE]이다.
 * - 표시가 [FramingHint.OK]로 바뀐 프레임부터 [FramingThresholds.readyHoldMs] 이상 이어지면 ready다.
 *   표시가 OK가 아니게 되면 바로 ready를 끄고 시간도 처음부터 다시 잰다.
 * - 직전 프레임을 기억하므로 카메라 연결(또는 화면 진입)마다 새 인스턴스를 만든다.
 */
class FramingStabilizer(private val thresholds: FramingThresholds = FramingThresholds()) {
    private var shown = FramingHint.NOT_VISIBLE
    private var candidate: FramingHint? = null
    private var candidateCount = 0
    private var okSinceMs: Long? = null

    fun update(hint: FramingHint, timestampMs: Long): FramingStatus {
        if (hint == shown) {
            candidate = null
            candidateCount = 0
        } else {
            candidateCount = if (hint == candidate) candidateCount + 1 else 1
            candidate = hint
            if (candidateCount >= thresholds.stableFrames) {
                shown = hint
                candidate = null
                candidateCount = 0
            }
        }

        okSinceMs = if (shown == FramingHint.OK) okSinceMs ?: timestampMs else null
        val ready = okSinceMs?.let { timestampMs - it >= thresholds.readyHoldMs } ?: false
        return FramingStatus(shown, ready)
    }
}
