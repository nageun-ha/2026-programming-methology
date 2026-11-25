package com.example.prm_project.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase

@Entity(tableName = "exercise_records", indices = [Index("epochDay")])
data class ExerciseRecordEntity(
    val epochDay: Long,
    val durationSec: Int,
    val judgedCount: Int,
    val successCount: Int,
    val softwareFailCount: Int,
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
)

@Dao
interface ExerciseRecordDao {
    @Insert
    suspend fun insert(record: ExerciseRecordEntity)

    @Query("SELECT * FROM exercise_records WHERE epochDay BETWEEN :from AND :to ORDER BY epochDay, id")
    suspend fun recordsBetween(from: Long, to: Long): List<ExerciseRecordEntity>
}

@Database(entities = [ExerciseRecordEntity::class], version = 1, exportSchema = false)
abstract class RecordDatabase : RoomDatabase() {
    abstract fun exerciseRecordDao(): ExerciseRecordDao

    companion object {
        private var instance: RecordDatabase? = null

        @Synchronized
        fun getInstance(context: Context): RecordDatabase =
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                RecordDatabase::class.java,
                "exercise-records.db",
            ).build().also { instance = it }
    }
}
