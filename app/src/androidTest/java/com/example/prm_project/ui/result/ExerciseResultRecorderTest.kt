package com.example.prm_project.ui.result

import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.example.prm_project.data.RecordDatabase
import com.example.prm_project.data.RecordRepository
import com.example.prm_project.model.ExerciseRecord
import com.example.prm_project.model.Grade
import java.time.LocalDate
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExerciseResultRecorderTest {
    @Test
    fun concurrentCompletionSavesOneRecordPerWorkout() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = Room.inMemoryDatabaseBuilder(context, RecordDatabase::class.java).build()
        try {
            val repository = RecordRepository(database.exerciseRecordDao())
            val date = LocalDate.of(2026, 11, 23)
            val record = ExerciseRecord(date, 90, 0, 0, 0, Grade.entries.associateWith { 0 })
            val recorder = ExerciseResultRecorder(repository)

            List(2) { async { recorder.save(record) } }.awaitAll()
            assertEquals(listOf(record), repository.recordsBetween(date, date))

            ExerciseResultRecorder(repository).save(record)
            assertEquals(listOf(record, record), repository.recordsBetween(date, date))
        } finally {
            database.close()
        }
    }

    @Test
    fun retriesFailedWriteAndOnlySavesOnce() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = Room.inMemoryDatabaseBuilder(context, RecordDatabase::class.java).build()
        try {
            val repository = RecordRepository(database.exerciseRecordDao())
            val date = LocalDate.of(2026, 11, 23)
            val record = ExerciseRecord(date, 90, 0, 0, 0)
            val recorder = ExerciseResultRecorder(repository)
            val sqlite = database.openHelper.writableDatabase
            sqlite.execSQL(
                """
                CREATE TRIGGER fail_exercise_record_insert BEFORE INSERT ON exercise_records
                BEGIN
                  SELECT RAISE(ABORT, 'test write failure');
                END
                """.trimIndent(),
            )

            val failure = runCatching { recorder.save(record) }.exceptionOrNull()
            assertTrue(failure is SQLiteConstraintException)
            assertEquals(emptyList<ExerciseRecord>(), repository.recordsBetween(date, date))

            sqlite.execSQL("DROP TRIGGER fail_exercise_record_insert")
            recorder.save(record)
            recorder.save(record)
            assertEquals(listOf(record), repository.recordsBetween(date, date))
        } finally {
            database.close()
        }
    }
}
