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
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Entity(tableName = "exercise_records", indices = [Index("epochDay")])
data class ExerciseRecordEntity(
    val epochDay: Long,
    val durationSec: Int,
    val judgedCount: Int,
    val successCount: Int,
    val softwareFailCount: Int,
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val perfectCount: Int? = null,
    val greatCount: Int? = null,
    val badCount: Int? = null,
    val missCount: Int? = null,
)

@Dao
interface ExerciseRecordDao {
    @Insert
    suspend fun insert(record: ExerciseRecordEntity)

    @Query("SELECT * FROM exercise_records WHERE epochDay BETWEEN :from AND :to ORDER BY epochDay, id")
    suspend fun recordsBetween(from: Long, to: Long): List<ExerciseRecordEntity>
}

@Database(entities = [ExerciseRecordEntity::class], version = 2, exportSchema = false)
abstract class RecordDatabase : RoomDatabase() {
    abstract fun exerciseRecordDao(): ExerciseRecordDao

    companion object {
        internal val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE exercise_records ADD COLUMN perfectCount INTEGER")
                db.execSQL("ALTER TABLE exercise_records ADD COLUMN greatCount INTEGER")
                db.execSQL("ALTER TABLE exercise_records ADD COLUMN badCount INTEGER")
                db.execSQL("ALTER TABLE exercise_records ADD COLUMN missCount INTEGER")
            }
        }

        private var instance: RecordDatabase? = null

        @Synchronized
        fun getInstance(context: Context): RecordDatabase =
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                RecordDatabase::class.java,
                "exercise-records.db",
            ).addMigrations(MIGRATION_1_2).build().also { instance = it }
    }
}
