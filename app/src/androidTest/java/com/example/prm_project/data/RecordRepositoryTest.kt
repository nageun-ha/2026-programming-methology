package com.example.prm_project.data

import android.content.Context
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.prm_project.model.ExerciseRecord
import com.example.prm_project.model.Grade
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class RecordRepositoryTest {
    @Test
    fun savesAndQueriesCompletedSessionsWithinInclusiveDates() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = Room.inMemoryDatabaseBuilder(context, RecordDatabase::class.java).build()
        try {
            val repository = RecordRepository(database.exerciseRecordDao())
            val first = ExerciseRecord(
                date = LocalDate.of(2024, 2, 1),
                durationSec = 90,
                judgedCount = 8,
                successCount = 5,
                softwareFailCount = 1,
                gradeCounts = mapOf(Grade.PERFECT to 2, Grade.GREAT to 2, Grade.BAD to 0, Grade.MISS to 3),
            )
            val sameDay = first.copy(durationSec = 120, judgedCount = 10, successCount = 7)
            val last = first.copy(date = LocalDate.of(2024, 2, 29))

            repository.save(last)
            repository.save(first.copy(date = first.date.minusDays(1)))
            repository.save(first)
            repository.save(sameDay)
            repository.save(last.copy(date = last.date.plusDays(1)))

            assertEquals(listOf(first, sameDay, last), repository.recordsBetween(first.date, last.date))
        } finally {
            database.close()
        }
    }

    @Test
    fun migrationPreservesRecordsWithoutInventingGradeCounts() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "record-migration-${UUID.randomUUID()}.db"
        val date = LocalDate.of(2024, 2, 1)
        try {
            context.openOrCreateDatabase(name, Context.MODE_PRIVATE, null).use { db ->
                db.execSQL(
                    """
                    CREATE TABLE exercise_records (
                      epochDay INTEGER NOT NULL,
                      durationSec INTEGER NOT NULL,
                      judgedCount INTEGER NOT NULL,
                      successCount INTEGER NOT NULL,
                      softwareFailCount INTEGER NOT NULL,
                      id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL
                    )
                    """.trimIndent(),
                )
                db.execSQL("CREATE INDEX index_exercise_records_epochDay ON exercise_records(epochDay)")
                db.execSQL("INSERT INTO exercise_records VALUES (${date.toEpochDay()}, 90, 8, 5, 1, 1)")
                db.version = 1
            }
            val database = Room.databaseBuilder(context, RecordDatabase::class.java, name)
                .addMigrations(RecordDatabase.MIGRATION_1_2)
                .build()
            try {
                val repository = RecordRepository(database.exerciseRecordDao())
                assertEquals(listOf(ExerciseRecord(date, 90, 8, 5, 1)), repository.recordsBetween(date, date))
            } finally {
                database.close()
            }
        } finally {
            context.deleteDatabase(name)
        }
    }
}
