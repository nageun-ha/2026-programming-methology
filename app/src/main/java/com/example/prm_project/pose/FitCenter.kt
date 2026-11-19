package com.example.prm_project.pose

import androidx.compose.ui.geometry.Offset
import kotlin.math.min

/**
 * [com.example.prm_project.model.PoseFrame] 좌표(0~1)를 미리보기 칸의 화면 좌표(px)로 바꾼다.
 *
 * 미리보기는 FIT_CENTER라 이미지를 비율 그대로 칸에 맞추고 남는 쪽에 여백을 둔다. 스켈레톤도 같은 계산을 해야
 * 영상 속 몸 위에 겹친다. 사용자 스켈레톤과 가이드 스켈레톤(#38)이 모두 이 함수로 그린다.
 * 거울 반전은 PoseFrame에서 이미 끝났으므로 여기서는 하지 않는다.
 */
class FitCenter(imageWidth: Int, imageHeight: Int, viewWidth: Float, viewHeight: Float) {
    val scale = min(viewWidth / imageWidth, viewHeight / imageHeight)
    private val drawnWidth = imageWidth * scale
    private val drawnHeight = imageHeight * scale
    private val offsetX = (viewWidth - drawnWidth) / 2
    private val offsetY = (viewHeight - drawnHeight) / 2

    fun toView(x: Float, y: Float) = Offset(offsetX + x * drawnWidth, offsetY + y * drawnHeight)
}
