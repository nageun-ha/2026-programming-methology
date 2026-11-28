package com.example.prm_project.judge

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.example.prm_project.data.RecordDatabase
import com.example.prm_project.data.RecordRepository
import com.example.prm_project.model.Chart
import com.example.prm_project.model.ChartItem
import com.example.prm_project.model.ExercisePhase
import com.example.prm_project.model.Grade
import com.example.prm_project.model.TargetPose
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Test

class RealExerciseSessionPersistenceTest {
    @Test
    fun gradeCountsFromFinishedSessionSurviveReopeningDatabase() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val databaseName = "session-grades-${UUID.randomUUID()}.db"
        val databaseBuilder = Room.databaseBuilder(context, RecordDatabase::class.java, databaseName)
        val date = LocalDate.of(2026, 11, 28)
        val session = RealExerciseSession(
            scope = this,
            frames = emptyFlow(),
            chart = Chart(120, listOf(ChartItem.Pose("a", 2), ChartItem.Rest(1), ChartItem.Pose("a", 1))),
            poses = mapOf("a" to TargetPose("a", "A", emptyMap(), 10f)),
            today = { date },
        )

        session.start()
        withTimeout(15_000) { session.state.first { it.phase == ExercisePhase.FINISHED } }
        val record = session.stop()
        assertEquals(3, record.judgedCount)
        assertEquals(3, record.successCount)
        assertEquals(3, record.softwareFailCount)
        assertEquals(
            mapOf(Grade.PERFECT to 0, Grade.GREAT to 3, Grade.BAD to 0, Grade.MISS to 0),
            record.gradeCounts,
        )

        try {
            val writer = databaseBuilder.build()
            try {
                RecordRepository(writer.exerciseRecordDao()).save(record)
            } finally {
                writer.close()
            }

            val reader = databaseBuilder.build()
            try {
                assertEquals(listOf(record), RecordRepository(reader.exerciseRecordDao()).recordsBetween(date, date))
            } finally {
                reader.close()
            }
        } finally {
            context.deleteDatabase(databaseName)
        }
    }
}
