package com.example.prm_project.ui.result

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.prm_project.data.score
import com.example.prm_project.model.ExerciseRecord
import com.example.prm_project.model.Grade
import com.example.prm_project.ui.theme.Prm_projectTheme
import java.text.NumberFormat
import java.time.LocalDate
import java.util.Locale

@Composable
fun ExerciseResultScreen(
    record: ExerciseRecord,
    modifier: Modifier = Modifier,
) {
    val scoreLabel = score(listOf(record))?.let { NumberFormat.getPercentInstance().format(it) }
        ?: "판독 기록 없음"

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Text("운동 결과", style = MaterialTheme.typography.headlineMedium)
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("점수", style = MaterialTheme.typography.titleMedium)
                Text(scoreLabel, style = MaterialTheme.typography.headlineLarge)
                Text("성공 ${record.successCount}회 / 판독 ${record.judgedCount}회")
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("등급별 횟수", style = MaterialTheme.typography.titleMedium)
            Grade.entries.forEach { grade ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(grade.name.lowercase(Locale.ROOT).replaceFirstChar { it.uppercaseChar() })
                    val count = record.gradeCounts[grade]
                    Text(if (count == null) "—" else "${count}회")
                }
            }
        }
    }
}

@Preview(name = "운동 결과", showBackground = true)
@Composable
internal fun ExerciseResultPreview() = PreviewResult(
    ExerciseRecord(
        date = LocalDate.of(2026, 11, 23),
        durationSec = 90,
        judgedCount = 12,
        successCount = 9,
        softwareFailCount = 1,
        gradeCounts = mapOf(Grade.PERFECT to 5, Grade.GREAT to 4, Grade.BAD to 1, Grade.MISS to 2),
    ),
)

@Preview(name = "판독 기록 없음", showBackground = true)
@Composable
internal fun EmptyExerciseResultPreview() = PreviewResult(ExerciseRecord(LocalDate.of(2026, 11, 23), 30, 0, 0, 0))

@Composable
private fun PreviewResult(record: ExerciseRecord) {
    Prm_projectTheme {
        Surface { ExerciseResultScreen(record) }
    }
}
