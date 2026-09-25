package home.babbakappa.bktl

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow

val AppScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val subjectName: String,
    val description: String,
    val creationDate: String,
    val expireDate: String,
    val isArchived: Boolean = false
)

@Entity(tableName = "subjects")
data class SubjectEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "name") val name: String
)

fun TaskEntity.toTask(): Task {
    val t = Task()
    t.SetId(id)
    t.CreateTask(subjectName, description, creationDate, expireDate)
    return t
}

fun Task.toEntity(isArchived: Boolean = false): TaskEntity =
    TaskEntity(
        id = GetId(),
        subjectName = GetSubjectName(),
        description = GetDescription(),
        creationDate = GetCreationDate(),
        expireDate = GetExpireDate(),
        isArchived = isArchived
    )

@Dao
interface TaskDao {

    @Query("SELECT * FROM tasks WHERE isArchived = 0 ORDER BY id")
    fun observeActive(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE isArchived = 1 ORDER BY id")
    fun observeArchived(): Flow<List<TaskEntity>>

    @Insert suspend fun insert(e: TaskEntity): Long
    @Update suspend fun update(e: TaskEntity)

    @Query("UPDATE tasks SET isArchived = 1 WHERE id = :id")
    suspend fun archive(id: Long)

    @Query("UPDATE tasks SET isArchived = 1 WHERE isArchived = 0")
    suspend fun archiveAll()

    @Query("UPDATE tasks SET isArchived = 0 WHERE id = :id")
    suspend fun unarchive(id: Long)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM tasks WHERE isArchived = 1")
    suspend fun deleteAllArchived()

    @Query("SELECT COUNT(*) FROM tasks")
    suspend fun countTasks(): Int

    // или сразу удобный булев:
    @Query("SELECT COUNT(*) > 0 FROM tasks")
    suspend fun hasAnyTask(): Boolean

    @Query("SELECT * FROM subjects ORDER BY name ASC")
    fun observeAllSubjects(): kotlinx.coroutines.flow.Flow<List<SubjectEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSubject(subject: SubjectEntity): Long

    @Query("SELECT COUNT(*) > 0 FROM subjects WHERE name = :name")
    suspend fun hasSubject(name: String): Boolean

    @Query("DELETE FROM subjects")
    suspend fun deleteAllSubjects()

    @Delete
    suspend fun deleteSubject(subject: SubjectEntity)

}


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

