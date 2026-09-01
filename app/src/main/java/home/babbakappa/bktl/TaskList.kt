//Файл TaskList.kt
//Файл класса TaskList, который содержит массив объектов Task, ну и по
//сути один из главных файлов ядра приложения. Ипользуется в UI.kt

package home.babbakappa.bktl

class TaskList {
    //Самый главный массив
    var MainArray = mutableListOf<Task>()

    //Функция создать задачу и добавить в массив
    fun CreateAndAddNewTask(subjname: String, desc: String, cd: String, ed: String) {
        var temp = Task()
        temp.CreateTask(subjname, desc, cd, ed)
        MainArray.add(temp)
    }

    //Добавить задачу в массив
    fun AddTask(t: Task) {
        MainArray.add(t)
    }

    //Удаление задачи по индексу
    //Хз зачем оно нужно теперь, но пусть останется
    fun DeleteTaskByIndex(idx: Int) {
        MainArray.removeAt(idx)
    }

    //Функция удалить все
    fun DeleteEverything() {
        MainArray.clear()
    }

    //Функция получить весь массив
    fun GetEntireTaskList(): List<Task> {
        return MainArray
    }

    //Функция получить задачу по индексу. Тоже хз зачем нужно
    fun GetTaskFromArrayByIndex(idx: Int): Task {
        return MainArray[idx]
    }

    //Функция задать массив
    fun SetArray(t: List<Task>) {
        MainArray = t.toMutableList()
    }

    //Функция удалить задачу по объекту
    fun DeleteTask(task: Task) {
        MainArray.remove(task)
    }

    fun CreateAndAddNewTaskWithReturn(subjname: String, gr: String, descr: String, cd: String): Task {
        val temp = Task()
        temp.CreateTask(subjname, gr, descr, cd)
        MainArray.add(temp)
        return temp
    }

    //Отредактировать задачу по индексу
    fun EditTaskByIndex(idx: Int, subject: String, gr: String, desc: String, cd: String) {
        val g = Task()
        g.CreateTask(subject, gr, desc, cd)
        MainArray[idx] = g
    }
}