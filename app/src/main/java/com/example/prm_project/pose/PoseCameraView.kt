package com.example.prm_project.pose

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.prm_project.model.DetectionState
import com.example.prm_project.model.Joint
import com.example.prm_project.model.PoseFrame
import kotlinx.coroutines.flow.Flow

/**
 * 전면 카메라 미리보기 위에 [frames]의 스켈레톤을 겹쳐 그린다. ③은 이 Composable을 배치만 한다.
 *
 * 감지 상태가 DETECTED가 아니면 스켈레톤을 흐리게 그린다. 관절이 없으면 아무것도 그리지 않는다.
 */
@Composable
fun PoseCameraView(frames: Flow<PoseFrame>, modifier: Modifier = Modifier) {
    val frame by frames.collectAsStateWithLifecycle(initialValue = null)
    Box(modifier) {
        CameraPreview(Modifier.fillMaxSize())
        frame?.let { SkeletonOverlay(it, Modifier.fillMaxSize()) }
    }
}

@Composable
private fun SkeletonOverlay(frame: PoseFrame, modifier: Modifier) {
    Canvas(modifier) {
        val fit = FitCenter(frame.imageWidth, frame.imageHeight, size.width, size.height)
        val points = frame.landmarks.mapValues { (_, it) -> fit.toView(it.x, it.y) }
        val alpha = if (frame.detection == DetectionState.DETECTED) 1f else 0.3f
        drawSkeleton(points, USER_COLOR.copy(alpha = alpha))
    }
}

/** 관절 점과 뼈대 선을 그린다. 두 끝 관절이 모두 있는 뼈만 그린다. 가이드 스켈레톤(#38)도 색만 바꿔 쓴다. */
internal fun DrawScope.drawSkeleton(points: Map<Joint, Offset>, color: Color) {
    val stroke = 4.dp.toPx()
    for ((a, b) in BONES) {
        val start = points[a] ?: continue
        val end = points[b] ?: continue
        drawLine(color, start, end, strokeWidth = stroke)
    }
    points.values.forEach { drawCircle(color, radius = stroke * 1.5f, center = it) }
}

private val USER_COLOR = Color(0xFF00E676)

private val BONES = listOf(
    Joint.LEFT_SHOULDER to Joint.RIGHT_SHOULDER,
    Joint.LEFT_SHOULDER to Joint.LEFT_ELBOW, Joint.LEFT_ELBOW to Joint.LEFT_WRIST,
    Joint.RIGHT_SHOULDER to Joint.RIGHT_ELBOW, Joint.RIGHT_ELBOW to Joint.RIGHT_WRIST,
    Joint.LEFT_SHOULDER to Joint.LEFT_HIP, Joint.RIGHT_SHOULDER to Joint.RIGHT_HIP,
    Joint.LEFT_HIP to Joint.RIGHT_HIP,
    Joint.LEFT_HIP to Joint.LEFT_KNEE, Joint.LEFT_KNEE to Joint.LEFT_ANKLE,
    Joint.RIGHT_HIP to Joint.RIGHT_KNEE, Joint.RIGHT_KNEE to Joint.RIGHT_ANKLE,
)

/** 반분할 칸(가로로 넓음)에서 여백과 스켈레톤 위치를 확인하는 미리보기. 카메라 대신 회색 배경이다. */
@Preview(widthDp = 360, heightDp = 400)
@Composable
private fun SkeletonPreview() {
    Box(Modifier.size(360.dp, 400.dp).background(Color.DarkGray)) {
        SkeletonOverlay(FakePoseSource().frameAt(2500), Modifier.fillMaxSize()) // 오른손 들기
    }
}
