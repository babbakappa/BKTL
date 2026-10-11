/* ToolsForUI.kt
* Здесь находятся самые разные вспомогательные инструменты для
* интерфейса приложения, например цвет даты дедлайна */

package home.babbakappa.bktl.other

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import android.content.Context
import android.net.Uri
import androidx.compose.runtime.Composable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.ui.graphics.Color
import java.io.BufferedReader
import java.io.InputStreamReader
import android.content.ContentValues
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import home.babbakappa.bktl.core.Task
import home.babbakappa.bktl.core.TaskList
import home.babbakappa.bktl.database.SubjectEntity
import home.babbakappa.bktl.database.TaskDao
import java.io.OutputStream
import java.io.OutputStreamWriter

private val deadlineFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")

//Функция для определения цвета чтобы подсветить дату дедлайна
@Composable
fun getDeadLineColor(other_date: String): Color {

    val today = LocalDate.now()
    val deadline = LocalDate.parse(other_date, deadlineFormatter)

    val daysLeft = ChronoUnit.DAYS.between(today, deadline)

    val rescolor: Color
    val isDark = isSystemInDarkTheme()

    when {
        daysLeft <= 0 -> rescolor = if (isDark) Color(0xFFFF00FF) else Color(0xFFDD00BB)
        daysLeft <= 1 -> rescolor = if (isDark) Color(0xFFFF0000) else Color(0xFFDD0000)
        daysLeft <= 2 -> rescolor = if (isDark) Color(0xFFFF7700) else Color(0xFFDD5500)
        daysLeft <= 4 -> rescolor = if (isDark) Color(0xFFFFCC00) else Color(0xFFDDAA00)
        else -> rescolor = MaterialTheme.colorScheme.onSurface
    }

    return rescolor
}

private fun escapeCsvField(field: String): String {
    val cleanField = field.replace("[NEWLINE]", "\n")
    if (cleanField.contains(",") || cleanField.contains("\"") || cleanField.contains("\n") || cleanField.contains("\r")) {
        return "\"" + cleanField.replace("\"", "\"\"") + "\""
    }
    return cleanField
}

suspend fun exportTasksToCsv(context: Context, tasks: List<Task>): Boolean {
    return withContext(Dispatchers.IO) {
        try {
            val timeStamp = SimpleDateFormat("dd.MM.yyyy_HH-mm-ss", Locale.getDefault()).format(Date())
            val fileName = "Задачи_$timeStamp.csv"

            // Функция для записи данных в поток
            val writeCsvToStream = { outputStream: OutputStream ->
                OutputStreamWriter(outputStream, Charsets.UTF_8).use { writer ->
                    // Записываем BOM для корректного отображения кириллицы в Excel
                    writer.write("\uFEFF")

                    // Заголовки
                    writer.write("Название,Описание,Дата создания,Дедлайн\n")

                    // Данные
                    tasks.forEach { task ->
                        val subject = escapeCsvField(task.subject_name)
                        val description = escapeCsvField(task.description)
                        val creationDate = escapeCsvField(task.creation_date)
                        val expireDate = escapeCsvField(task.expire_date)

                        writer.write("$subject,$description,$creationDate,$expireDate\n")
                    }
                }
            }

            // Запись в зависимости от версии Android
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val contentValues = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                    put(MediaStore.Downloads.MIME_TYPE, "text/csv")
                    put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }

                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    resolver.openOutputStream(uri)?.use { writeCsvToStream(it) }

                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Файл сохранен в Загрузки: $fileName", Toast.LENGTH_LONG).show()
                    }
                    return@withContext true
                }
            } else {
                val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!downloadDir.exists()) {
                    downloadDir.mkdirs()
                }
                val file = File(downloadDir, fileName)
                FileOutputStream(file).use { writeCsvToStream(it) }

                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Файл сохранен в Загрузки: $fileName", Toast.LENGTH_LONG).show()
                }
                return@withContext true
            }
            false
        } catch (e: Exception) {
            e.printStackTrace()
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Ошибка при сохранении файла", Toast.LENGTH_SHORT).show()
            }
            false
        }
    }
}


suspend fun importTasksFromCsv(context: Context, fileUri: Uri, taskList: TaskList): Boolean {
    return withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(fileUri) ?: return@withContext false

            BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { reader ->
                var line: String? = reader.readLine() // Читаем первую строку (заголовки)
                if (line == null) return@withContext false

                // Пропускаем BOM, если он есть в заголовке
                if (line!!.startsWith("\uFEFF")) {
                    line = line!!.substring(1)
                }

                // Парсинг CSV с учетом возможных кавычек и переносов строк внутри полей
                val currentFields = mutableListOf<String>()
                var insideQuotes = false
                val fieldBuilder = StringBuilder()

                while (reader.readLine().also { line = it } != null) {
                    var i = 0
                    while (i < line!!.length) {
                        val c = line!![i]
                        if (c == '"') {
                            if (insideQuotes && i + 1 < line!!.length && line!![i + 1] == '"') {
                                // Экранированная кавычка "" -> превращаем в одиночную "
                                fieldBuilder.append('"')
                                i++
                            } else {
                                // Переключаем режим нахождения внутри кавычек
                                insideQuotes = !insideQuotes
                            }
                        } else if (c == ',' && !insideQuotes) {
                            // Конец поля
                            currentFields.add(fieldBuilder.toString().trim())
                            fieldBuilder.setLength(0)
                        } else {
                            fieldBuilder.append(c)
                        }
                        i++
                    }

                    if (insideQuotes) {
                        // Если строка закончилась, но кавычка не закрыта, значит перенос строки был внутри поля
                        fieldBuilder.append("\n")
                    } else {
                        // Конец записи строки CSV
                        currentFields.add(fieldBuilder.toString().trim())
                        fieldBuilder.setLength(0)

                        // Проверяем, что считались все 4 колонки
                        if (currentFields.size >= 4) {
                            val subject = currentFields[0]
                            val description = currentFields[1]
                            val creationDate = currentFields[2]
                            val expireDate = currentFields[3]

                            if (subject.isNotBlank() && description.isNotBlank() && creationDate.isNotBlank() && expireDate.isNotBlank()) {
                                val realTask = taskList.CreateAndAddNewTaskWithReturn(subject, description, creationDate, expireDate)

                                // Переключаемся на Main поток для вызова UI-планировщика уведомлений
                                withContext(Dispatchers.Main) {
                                    NotificationHelper.scheduleNotification(realTask, context)
                                }
                            }
                        }
                        currentFields.clear()
                    }
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}

suspend fun addSubjectsRaw(rawText: String, dao: TaskDao) {
    val lines = rawText.split(Regex("[\n,]+"))
    lines.forEach { line ->
        val trimmed = line.trim()
        if (trimmed.isNotBlank() && !dao.hasSubject(trimmed)) {
            dao.insertSubject(SubjectEntity(name = trimmed))
        }
    }
}

suspend fun importSubjectsFromTxt(context: Context, uri: Uri, dao: TaskDao): Boolean {
    return try {
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            BufferedReader(InputStreamReader(inputStream)).use { reader ->
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    val trimmed = line?.trim() ?: ""
                    if (trimmed.isNotBlank() && !dao.hasSubject(trimmed)) {
                        dao.insertSubject(SubjectEntity(name = trimmed))
                    }
                }
            }
        }
        true
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}