//Файл FileSys.kt
//Этот файл содержит функции для взаимодействия с кастомными БД (которые на
//самом деле обычные текстовые файлы, но это не важно), универсальны и для
//основной БД, и для архивной БД

package home.babbakappa.bktl
import java.io.File //классика
import java.io.IOException

//Константные пути к БДшкам
val DBFilePath = "DataBase.mydb"
val ArchiveFilePath = "Archive.mydb"

//Сохранение массива в файл
fun SaveArrayToFile(arr: List<Task>, fp: String) {
    val targetFile = File(fp)
    val tempFile = File("$fp.tmp")
    val sb = StringBuilder()
    sb.append(arr.size).append("\n")
    for (obj in arr) {
        sb.append(obj.GetSubjectName()).append("\n")
        sb.append(obj.GetDescription()).append("\n")
        sb.append(obj.GetCreationDate()).append("\n")
        sb.append(obj.GetExpireDate()).append("\n")
    }
    try {
        //Записываем данные во временный файл
        tempFile.writeText(sb.toString())

        //Если основной файл существует, удаляем его, чтобы освободить место
        if (targetFile.exists()) {
            targetFile.delete()
        }

        //Переименовываем временный файл в основной (это атомарная операция)
        if (!tempFile.renameTo(targetFile)) {
            // Если переименование не удалось (например, из-за прав доступа),
            // просто перезаписываем основной файл напрямую, раз мы уже дошли до этого места
            targetFile.writeText(sb.toString())
            tempFile.delete() // И подчищаем за собой временный
        }

    } catch (error: IOException) {
        // Если что-то пошло не так (диск переполнен, нет прав и т.д.)
        // В первую очередь подчищаем временный файл, чтобы не засорять память
        if (tempFile.exists()) {
            tempFile.delete()
        }
        // Выводим ошибку в консоль (удобно для отладки)
        println("Ошибка сохранения файла: ${error.message}")
        // В идеале здесь стоит сделать уведомление для пользователя в UI
    }
}

//Функция загрузки массива из файла
//Работает по принципу: первая строка - общее число задач, следующие
//каждые четыре строки - одна задача, содержащая название предмета,
//описание задачи, дату создания и дату дедлайна (каждое на отдельной
//строке)
fun LoadArrayFromFile(fp: String): List<Task> {
    val file = File(fp)
    if (!file.exists()) {
        return mutableListOf()
    }
    val lines = file.readLines()
    if (lines.isEmpty()) {
        return mutableListOf()
    }
    val n = lines[0].toIntOrNull() ?: 0
    val result = mutableListOf<Task>()
    for (i in 0 until n) {
        // Каждая задача занимает 4 строки, начиная с индекса 1
        val base = 1 + i * 4
        if (base + 3 >= lines.size) break
        val sn = lines[base]
        val desc = lines[base + 1]
        val cd = lines[base + 2]
        val ed = lines[base + 3]
        val t = Task()
        t.CreateTask(sn, desc, cd, ed)
        result.add(t)
    }
    return result
}