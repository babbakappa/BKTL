package home.babbakappa.bktl

import android.content.Context
import androidx.room.*
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
}


@Database(entities = [TaskEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null
        fun get(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "tasks.db"
                ).fallbackToDestructiveMigration().build().also { INSTANCE = it }
            }
    }
}

