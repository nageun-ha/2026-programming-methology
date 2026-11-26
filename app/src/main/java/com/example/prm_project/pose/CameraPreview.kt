package com.example.prm_project.pose

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.annotation.StringRes
import androidx.camera.core.CameraSelector
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.prm_project.R

/**
 * 전면 카메라 미리보기. 카메라 권한 요청·거절 안내와 전면 카메라가 없는 기기 안내까지 여기서 처리한다.
 *
 * - 미리보기는 잘라내지 않는 FIT_CENTER로 그린다. 기본값 FILL_CENTER는 가장자리를 잘라 스켈레톤이 어긋난다(#19).
 * - 미리보기는 전면 카메라라 거울처럼 좌우 반전돼 보인다. [com.example.prm_project.model.PoseFrame] 좌표 규칙과 같다.
 * - 카메라는 이 Composable이 화면에 있는 동안, 그리고 화면이 켜져 있는 동안만 켜진다.
 */
@Composable
fun CameraPreview(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var granted by remember { mutableStateOf(context.hasCameraPermission()) }
    var denied by rememberSaveable { mutableStateOf(false) } // 화면 회전 뒤에도 거절 안내 유지
    val launcher = rememberLauncherForActivityResult(RequestPermission()) { ok ->
        granted = ok
        denied = !ok
    }
    // 설정에서 권한을 바꾸고 돌아오면 다시 확인한다
    LifecycleResumeEffect(Unit) {
        granted = context.hasCameraPermission()
        onPauseOrDispose {}
    }

    when {
        !context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT) ->
            Message(modifier, R.string.camera_front_missing)
        granted -> FrontCameraView(modifier)
        denied -> Message(modifier, R.string.camera_permission_denied, R.string.camera_open_settings) {
            context.startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
        else -> Message(modifier, R.string.camera_permission_needed, R.string.camera_allow) {
            launcher.launch(Manifest.permission.CAMERA)
        }
    }
}

@Composable
private fun FrontCameraView(modifier: Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val controller = remember {
        val ratio4to3 = ResolutionSelector.Builder()
            .setAspectRatioStrategy(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY)
            .build()
        LifecycleCameraController(context).apply {
            cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
            setEnabledUseCases(0) // 미리보기만. 분석(IMAGE_ANALYSIS)은 #25에서 켠다
            // 분석 프레임과 미리보기 비율이 다르면 좌표가 맞아도 스켈레톤이 어긋나므로 둘 다 4:3으로 고정
            previewResolutionSelector = ratio4to3
            imageAnalysisResolutionSelector = ratio4to3
        }
    }
    DisposableEffect(lifecycleOwner) {
        controller.bindToLifecycle(lifecycleOwner)
        onDispose { controller.unbind() } // 화면에서 빠지면 Activity가 살아 있어도 카메라를 끈다
    }
    AndroidView(
        factory = {
            PreviewView(it).apply {
                scaleType = PreviewView.ScaleType.FIT_CENTER
                // 기본 SurfaceView는 Compose 위 겹침(스켈레톤)·레이아웃 애니메이션에서 깨질 수 있어 TextureView로 둔다
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                this.controller = controller
            }
        },
        modifier = modifier,
    )
}

@Composable
private fun Message(
    modifier: Modifier,
    @StringRes text: Int,
    @StringRes action: Int? = null,
    onAction: () -> Unit = {},
) {
    // Surface: 배치되는 칸의 배경색과 상관없이 테마 배경·글자색으로 읽히게 한다
    Surface(modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(stringResource(text), textAlign = TextAlign.Center)
            if (action != null) Button(onClick = onAction) { Text(stringResource(action)) }
        }
    }
}

private fun Context.hasCameraPermission() =
    ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

@Preview(showBackground = true)
@Composable
private fun DeniedPreview() {
    Message(Modifier, R.string.camera_permission_denied, R.string.camera_open_settings)
}
