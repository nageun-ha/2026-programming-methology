package com.example.prm_project.ui.exercise

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.prm_project.judge.FakeExerciseSession
import com.example.prm_project.model.ExercisePhase
import com.example.prm_project.model.ExerciseRecord
import com.example.prm_project.model.ExerciseState
import com.example.prm_project.ui.theme.Prm_projectTheme

/** 운동 화면. 위 55% 카메라 슬롯(#19), 아래 45% 채보 영역(#30)으로 나눈다. */
@Composable
fun ExerciseScreen(
    onFinished: (ExerciseRecord) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ExerciseViewModel = viewModel(factory = ExerciseViewModel.FakeFactory),
    cameraContent: @Composable (Modifier) -> Unit = { CameraPlaceholder(it) },
) {
    val state by viewModel.state.collectAsState()
    val record by viewModel.record.collectAsState()
    LaunchedEffect(Unit) { viewModel.start() }
    LaunchedEffect(record) { record?.let(onFinished) }

    val progress = if (viewModel.totalBeats == 0) 0f else state.beatIndex.toFloat() / viewModel.totalBeats
    ExerciseLayout(state, progress, onStop = viewModel::finish, modifier, cameraContent)
}

/** 상태만 받아 그리는 위아래 반분할 레이아웃. */
@Composable
fun ExerciseLayout(
    state: ExerciseState,
    progress: Float,
    onStop: () -> Unit,
    modifier: Modifier = Modifier,
    cameraContent: @Composable (Modifier) -> Unit = { CameraPlaceholder(it) },
) {
    Column(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Box(Modifier.fillMaxWidth().weight(0.55f)) {
            cameraContent(Modifier.fillMaxSize())
            TopBar(progress, onStop, Modifier.align(Alignment.TopCenter))
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        Box(
            Modifier.fillMaxWidth().weight(0.45f).background(MaterialTheme.colorScheme.surface),
            contentAlignment = Alignment.Center,
        ) {
            Text("${state.phase} · beat ${state.beatIndex}", color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
private fun TopBar(progress: Float, onStop: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LinearProgressIndicator(
            progress = { progress.coerceIn(0f, 1f) },
            modifier = Modifier.weight(1f).height(4.dp),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.outline,
        )
        IconButton(onClick = onStop) { Text("II", color = Color.White) }
    }
}

@Composable
private fun CameraPlaceholder(modifier: Modifier = Modifier) {
    Box(modifier.background(Color(0xFF3A3A3C)), contentAlignment = Alignment.Center) {
        Text("Camera (#19)", color = Color(0xFF8E8E93))
    }
}

private val PreviewState = ExerciseState.INITIAL.copy(phase = ExercisePhase.PLAYING, beatIndex = 4)
private val PreviewTotal = FakeExerciseSession.DEFAULT_CHART.items.sumOf { it.beats }

@Preview(name = "Light", widthDp = 360, heightDp = 780)
@Composable
private fun ExerciseLightPreview() = Prm_projectTheme(darkTheme = false) {
    ExerciseLayout(PreviewState, progress = 4f / PreviewTotal, onStop = {})
}

@Preview(name = "Dark", widthDp = 360, heightDp = 780)
@Composable
private fun ExerciseDarkPreview() = Prm_projectTheme(darkTheme = true) {
    ExerciseLayout(PreviewState, progress = 4f / PreviewTotal, onStop = {})
}
