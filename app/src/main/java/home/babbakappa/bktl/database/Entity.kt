/* Entity.kt
* Файл, в котором находится таблица БД Room,
* ее инициализация, ее ентити и SQL-запросы
* для нее */

package home.babbakappa.bktl.database

import androidx.room.*
import home.babbakappa.bktl.core.Task
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

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
    val t = Task(subjectName, description, creationDate, expireDate)
    t.id = id
    return t
}

fun Task.toEntity(isArchived: Boolean = false): TaskEntity =
    TaskEntity(
        id = id,
        subjectName = subject_name,
        description = description,
        creationDate = creation_date,
        expireDate = expire_date,
        isArchived = isArchived
    )


