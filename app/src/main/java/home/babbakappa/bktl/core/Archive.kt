/* Archive.kt
* Этот файл - репозиторий для удаленных (помещенных в
* архив) задач */

package home.babbakappa.bktl.core

import home.babbakappa.bktl.database.TaskDao
import home.babbakappa.bktl.database.toEntity
import home.babbakappa.bktl.database.toTask
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class Archive(private val dao: TaskDao) {

    val archiveFlow: Flow<List<Task>> =
        dao.observeArchived().map { list -> list.map { it.toTask() } }

    suspend fun AddTaskToArchive(obj: Task) {
        if (obj.id != 0L) dao.archive(obj.id)
        else dao.insert(obj.toEntity().copy(isArchived = true))
    }

    suspend fun RemoveFromArchive(task: Task) {
        dao.unarchive(task.id)
    }

    suspend fun DeleteEverything() {
        dao.deleteAllArchived()
    }

    suspend fun DeleteForever(task: Task) {
        dao.deleteById(task.id)
    }
}