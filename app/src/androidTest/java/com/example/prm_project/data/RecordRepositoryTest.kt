package com.example.prm_project.data

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.prm_project.model.ExerciseRecord
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RecordRepositoryTest {
    @Test
    fun savesAndQueriesCompletedSessionsWithinInclusiveDates() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = Room.inMemoryDatabaseBuilder(context, RecordDatabase::class.java).build()
        try {
            val repository = RecordRepository(database.exerciseRecordDao())
            val first = ExerciseRecord(LocalDate.of(2024, 2, 1), 90, 8, 5, 1)
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
}
