package home.babbakappa.bktl

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class Archive(private val dao: TaskDao) {

    val archiveFlow: Flow<List<Task>> =
        dao.observeArchived().map { list -> list.map { it.toTask() } }

    fun AddTaskToArchive(obj: Task) {
        AppScope.launch {
            if (obj.GetId() != 0L) dao.archive(obj.GetId())
            else dao.insert(obj.toEntity().copy(isArchived = true))
        }
    }

    fun RemoveFromArchive(task: Task) {
        AppScope.launch { dao.unarchive(task.GetId()) }
    }

    fun DeleteEverything() {
        AppScope.launch { dao.deleteAllArchived() }
    }

    fun DeleteForever(task: Task) {
        AppScope.launch { dao.deleteById(task.GetId()) }
    }
}