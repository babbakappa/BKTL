package home.babbakappa.bktl.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

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
    fun observeAllSubjects(): Flow<List<SubjectEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSubject(subject: SubjectEntity): Long

    @Query("SELECT COUNT(*) > 0 FROM subjects WHERE name = :name")
    suspend fun hasSubject(name: String): Boolean

    @Query("DELETE FROM subjects")
    suspend fun deleteAllSubjects()

    @Delete
    suspend fun deleteSubject(subject: SubjectEntity)

    @Query("SELECT * FROM tasks WHERE isArchived = 0 ORDER BY subjectName")
    fun sortByNames(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE isArchived = 0 ORDER BY substr(expireDate, 7, 4) ASC, substr(expireDate, 4, 2) ASC, substr(expireDate, 1, 2) ASC ")
    fun sortByExpireDate(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE isArchived = 0 AND lower(subjectName) LIKE '%' || lower(:specificSubject) || '%'")
    fun onlySpecificSubjects(specificSubject: String): Flow<List<TaskEntity>>

}
