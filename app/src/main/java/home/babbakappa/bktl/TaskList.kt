//Файл TaskList.kt
//Файл класса TaskList, который содержит массив объектов Task, ну и по
//сути один из главных файлов ядра приложения. Ипользуется в MainUI.kt

package home.babbakappa.bktl

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map


class TaskList(private val dao: TaskDao) {

    val tasksFlow: Flow<List<Task>> =
        dao.observeActive().map { list -> list.map { it.toTask() } }

    suspend fun CreateAndAddNewTask(subjname: String, desc: String, cd: String, ed: String) {
        dao.insert(TaskEntity(subjectName = subjname, description = desc, creationDate = cd, expireDate = ed))
    }

    suspend fun CreateAndAddNewTaskWithReturn(subjname: String, descr: String, cd: String, ed: String): Task {
        val temp = Task()
        temp.CreateTask(subjname, descr, cd, ed)
        val newId = dao.insert(temp.toEntity()) // Room выполняет это на фоновом потоке
        temp.SetId(newId)
        return temp
    }

    suspend fun AddTask(t: Task) {
        if (t.GetId() != 0L) dao.unarchive(t.GetId())
        else dao.insert(t.toEntity())
    }

    suspend fun DeleteTask(task: Task) {
        dao.archive(task.GetId())
    }

    suspend fun DeleteEverything() {
        dao.archiveAll()
    }

    suspend fun EditTask(task: Task, subject: String, desc: String, cd: String, ed: String) {
        dao.update(TaskEntity(id = task.GetId(), subjectName = subject, description = desc, creationDate = cd, expireDate = ed, isArchived = false))
    }
}
