//Файл TaskList.kt
//Файл класса TaskList, который содержит массив объектов Task, ну и по
//сути один из главных файлов ядра приложения. Ипользуется в MainUI.kt

package home.babbakappa.bktl

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class TaskList(private val dao: TaskDao) {

    val tasksFlow: Flow<List<Task>> =
        dao.observeActive().map { list -> list.map { it.toTask() } }

    fun CreateAndAddNewTask(subjname: String, desc: String, cd: String, ed: String) {
        AppScope.launch {
            dao.insert(TaskEntity(
                subjectName = subjname,
                description = desc,
                creationDate = cd,
                expireDate = ed
            ))
        }
    }

    suspend fun CreateAndAddNewTaskWithReturn(subjname: String, descr: String, cd: String, ed: String): Task = withContext(
        Dispatchers.IO) {
        val temp = Task()
        temp.CreateTask(subjname, descr, cd, ed)

        // Дожидаемся реального ID от Room
        val newId = dao.insert(temp.toEntity())
        temp.SetId(newId)

        return@withContext temp
    }

    fun AddTask(t: Task) {
        AppScope.launch {
            if (t.GetId() != 0L) dao.unarchive(t.GetId())
            else dao.insert(t.toEntity())
        }
    }

    fun DeleteTask(task: Task) {
        AppScope.launch { dao.archive(task.GetId()) }
    }

    fun DeleteEverything() {
        AppScope.launch { dao.archiveAll() }
    }

    // Заменяем EditTaskByIndex на EditTask (передаём сам объект — так надёжнее с Flow)
    fun EditTask(task: Task, subject: String, desc: String, cd: String, ed: String) {
        AppScope.launch {
            dao.update(TaskEntity(
                id = task.GetId(),
                subjectName = subject,
                description = desc,
                creationDate = cd,
                expireDate = ed,
                isArchived = false
            ))
        }
    }
}
