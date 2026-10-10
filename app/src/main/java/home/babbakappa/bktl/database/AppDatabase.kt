package home.babbakappa.bktl.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [TaskEntity::class, SubjectEntity::class], version = 3, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Выполняем SQL-запрос создания новой таблицы предметов.
                // Строго соблюдаем типы Room: id (INTEGER NOT NULL), name (TEXT NOT NULL)
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `subjects` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                        `name` TEXT NOT NULL
                    )
                """.trimIndent())
            }
        }

        fun get(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "tasks.db"
                )
                    // Подключаем скрипт перехода без потери данных
                    .addMigrations(MIGRATION_2_3)
                    // Оставляем деструктивный метод только на случай непредвиденных сбоев
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
