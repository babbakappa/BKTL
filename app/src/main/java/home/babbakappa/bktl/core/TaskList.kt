/* TaskList.kt
* Репозиторий для объектов Task */

package home.babbakappa.bktl.core

import home.babbakappa.bktl.database.TaskDao
import home.babbakappa.bktl.database.TaskEntity
import home.babbakappa.bktl.database.toEntity
import home.babbakappa.bktl.database.toTask
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map


class TaskList(private val dao: TaskDao) {

    val tasksFlow: Flow<List<Task>> =
        dao.observeActive().map { list -> list.map { it.toTask() } }

    suspend fun CreateAndAddNewTask(subjname: String, desc: String, cd: String, ed: String) {
        dao.insert(
            TaskEntity(
                subjectName = subjname,
                description = desc,
                creationDate = cd,
                expireDate = ed
            )
        )
    }

    suspend fun CreateAndAddNewTaskWithReturn(subjname: String, descr: String, cd: String, ed: String): Task {
        val temp = Task(subjname, descr, cd, ed)
        val newId = dao.insert(temp.toEntity()) // Room выполняет это на фоновом потоке
        temp.id = newId
        return temp
    }

    suspend fun AddTask(t: Task) {
        if (t.id != 0L) dao.unarchive(t.id)
        else dao.insert(t.toEntity())
    }

    suspend fun DeleteTask(task: Task) {
        dao.archive(task.id)
    }

    suspend fun DeleteEverything() {
        dao.archiveAll()
    }

    suspend fun EditTask(task: Task, subject: String, desc: String, cd: String, ed: String) {
        // Обновляем объект в памяти, чтобы scheduleNotification получил свежие данные
        task.subject_name = subject
        task.description = desc
        task.creation_date = cd
        task.expire_date = ed
        dao.update(task.toEntity())
    }
}
