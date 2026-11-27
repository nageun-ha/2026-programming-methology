package com.example.prm_project.pose

import android.content.Context
import android.os.SystemClock
import android.util.Log
import androidx.camera.core.ImageAnalysis
import com.example.prm_project.model.PoseFrame
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.ImageProcessingOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlin.math.min

/**
 * 전면 카메라 분석 프레임에서 포즈를 찾아 [PoseFrame]으로 내보내는 실제 [PoseSource].
 *
 * MediaPipe Pose Landmarker(full)를 LIVE_STREAM 모드로 돌린다(#18). [analyzer]를 [PoseCameraView]에 넘기면
 * 미리보기와 같은 카메라에서 프레임을 받는다. 다 쓰면 [close]한다.
 *
 * ```
 * val source = CameraPoseSource(context)
 * PoseCameraView(source.frames, analyzer = source.analyzer)
 * ```
 *
 * - [PoseFrame.timestampMs]: 프레임을 받은 순간의 [clock](elapsedRealtime). CameraX 타임스탬프는 기기마다 기준이 달라 쓰지 않는다.
 * - 좌표: MediaPipe는 회전 전 버퍼 기준 좌표를 주므로 [FrameMapper]로 회전·거울 반전한다.
 * - 감지 상태: [DetectionClassifier]가 정한다. 처리가 밀리면 오래된 프레임은 버린다.
 */
class CameraPoseSource(
    context: Context,
    private val clock: () -> Long = SystemClock::elapsedRealtime,
) : PoseSource, AutoCloseable {

    private val output = MutableSharedFlow<PoseFrame>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    override val frames: Flow<PoseFrame> = output

    private val classifier = DetectionClassifier() // 결과 콜백 스레드 하나에서만 쓴다
    @Volatile private var rotationDegrees = 0 // 기기를 돌릴 때만 바뀐다
    private var lastTimestampMs = -1L

    private val landmarker = PoseLandmarker.createFromOptions(
        context,
        PoseLandmarker.PoseLandmarkerOptions.builder()
            .setBaseOptions(BaseOptions.builder().setModelAssetPath(MODEL).build())
            .setRunningMode(RunningMode.LIVE_STREAM)
            .setNumPoses(1)
            .setResultListener(::onResult)
            .setErrorListener { Log.e(TAG, "pose detection failed", it) }
            .build(),
    )

    /** 분석 프레임을 비트맵으로 복사하고 바로 닫는다(닫지 않으면 다음 프레임이 오지 않는다). */
    val analyzer = ImageAnalysis.Analyzer { proxy ->
        val timestampMs = clock()
        val bitmap = proxy.use {
            rotationDegrees = it.imageInfo.rotationDegrees
            it.toBitmap()
        }
        if (timestampMs <= lastTimestampMs) return@Analyzer // MediaPipe는 시각이 늘어나야 한다
        lastTimestampMs = timestampMs
        val options = ImageProcessingOptions.builder().setRotationDegrees(rotationDegrees).build()
        landmarker.detectAsync(BitmapImageBuilder(bitmap).build(), options, timestampMs)
    }

    private fun onResult(result: PoseLandmarkerResult, input: MPImage) {
        val mapper = FrameMapper(input.width, input.height, rotationDegrees, mirror = true)
        val raw = result.landmarks().firstOrNull().orEmpty().mapIndexed { index, point ->
            // presence: 화면 안일 확률, visibility: 가려지지 않았을 확률. 둘 다 높아야 "화면 안에 보인다"
            val likelihood = min(point.presence().orElse(0f), point.visibility().orElse(0f))
            RawLandmark(index, point.x() * input.width, point.y() * input.height, likelihood)
        }
        val timestampMs = result.timestampMs()
        Log.d(TAG, "latency=${clock() - timestampMs}ms") // 프레임을 받은 순간부터 결과까지(변환·추론 포함)
        val classified = classifier.classify(mapper.map(raw), timestampMs)
        output.tryEmit(
            PoseFrame(
                classified.landmarks, mapper.imageWidth, mapper.imageHeight, timestampMs, classified.state,
                classified.outOfFrame, classified.lowConfidence, classified.edge,
            ),
        )
    }

    override fun close() = landmarker.close()

    private companion object {
        const val TAG = "CameraPoseSource"
        const val MODEL = "pose_landmarker_full.task"
    }
}
