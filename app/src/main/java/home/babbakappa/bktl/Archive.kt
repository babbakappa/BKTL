//Файл Archive.kt
//Этот файл содержит класс Archive, предназаченный для хранения
//удаленных задач. Используется в UI.kt

package home.babbakappa.bktl

//Класс архива данных
class Archive {

    //Главный массив
    var ArchiveArray = mutableListOf<Task>()

    //Добавление задачи
    fun AddTaskToArchive(obj: Task) {
        ArchiveArray.add(obj)
    }

    //Получение всего архива (да, вот так)
    fun GetEntireArchive(): List<Task> {
        return ArchiveArray
    }

    //Функция удалить все
    fun DeleteEverything() {
        ArchiveArray.clear()
    }

    //Функция перезаписать архив
    fun SetArchive(t: List<Task>) {
        ArchiveArray = t.toMutableList()
    }

    //Удаление из архива по объекту
    fun RemoveFromArchive(task: Task) {
        ArchiveArray.remove(task)
    }
}