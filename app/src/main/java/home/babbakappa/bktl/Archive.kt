package home.babbakappa.bktl

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class Archive(private val dao: TaskDao) {

    val archiveFlow: Flow<List<Task>> =
        dao.observeArchived().map { list -> list.map { it.toTask() } }

    suspend fun AddTaskToArchive(obj: Task) {
        if (obj.GetId() != 0L) dao.archive(obj.GetId())
        else dao.insert(obj.toEntity().copy(isArchived = true))
    }

    suspend fun RemoveFromArchive(task: Task) {
        dao.unarchive(task.GetId())
    }

    suspend fun DeleteEverything() {
        dao.deleteAllArchived()
    }

    suspend fun DeleteForever(task: Task) {
        dao.deleteById(task.GetId())
    }
}