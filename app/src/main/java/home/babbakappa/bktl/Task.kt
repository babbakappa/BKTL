//Файл Task.kt
//Этот файл содержит класс Task, объект которого и явлется отдельным
//заданием. Используется в нескольких файлах проекта

package home.babbakappa.bktl

import androidx.compose.runtime.Immutable

@Immutable
class Task {

    //Главные поля
    var subject_name = ""
    var description = ""
    var creation_date = ""
    var expire_date = ""

    //Функция создать задачу
    fun CreateTask(n_subjname: String, n_desc: String, n_crtdt: String, n_expdt: String) {
        subject_name = n_subjname
        SetDescription(n_desc)
        creation_date = n_crtdt
        expire_date = n_expdt
    }

    //Сеттеры
    fun SetSubjectName(name: String) {
        subject_name = name
    }

    fun SetDescription(desc: String) {
        description = desc.replace("\n", "[NEWLINE]") //для обработки переносов
    }

    fun SetCreationDate(cd: String) {
        creation_date = cd
    }

    fun SetExpireDate(ed: String) {
        expire_date = ed
    }

    //Геттеры
    fun GetSubjectName(): String {
        return subject_name
    }

    fun GetDescription(): String {
        return description
    }

    fun GetCreationDate(): String {
        return creation_date
    }

    fun GetExpireDate(): String {
        return expire_date
    }
}