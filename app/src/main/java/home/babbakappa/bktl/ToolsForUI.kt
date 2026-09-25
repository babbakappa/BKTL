//Файл ToolsForUI.kt
//Функции этого файла используются в MainUI.kt, нужны для выполнения
//некоторых функций, таких как определения разницы между датами или
//сохранения отчета в Excel

package home.babbakappa.bktl

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import android.content.Context
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.ui.graphics.Color
import org.apache.poi.ss.usermodel.WorkbookFactory
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader

private val deadlineFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")

//Функция для определения цвета чтобы подсветить дату дедлайна
@Composable
fun getDeadLineColor(other_date: String, pattern: String = "dd.MM.yyyy"): Color {

    val today = LocalDate.now()
    val deadline = LocalDate.parse(other_date, deadlineFormatter)

    val daysLeft = ChronoUnit.DAYS.between(today, deadline)

    val rescolor: Color

    when {
        daysLeft < 0 -> rescolor = SetPurpleColor() //Просрочено совсем
        daysLeft <= 2 -> rescolor = SetRedColor() //Горит (0-2 дня)
        daysLeft <= 4 -> rescolor = SetYellowColor() //Предупреждение (3-4 дней)
        else -> rescolor = SetTextColor()
    }

    return rescolor
}



//Функция для экспорта отчета в формате xlsx
suspend fun exportTasksToExcel(context: Context, tasks: List<Task>): Uri? {
    return withContext(Dispatchers.IO) {
        try {
            val workbook = XSSFWorkbook()
            val sheet = workbook.createSheet("Задачи")

            // Заголовки
            val headerRow = sheet.createRow(0)
            val headers = arrayOf("Предмет", "Описание", "Дата создания", "Дедлайн")
            headers.forEachIndexed { index, title ->
                val cell = headerRow.createCell(index)
                cell.setCellValue(title)
                // Можно сделать жирным
                val style = workbook.createCellStyle()
                val font = workbook.createFont()
                font.bold = true
                style.setFont(font)
                cell.cellStyle = style
            }

            // Данные
            tasks.forEachIndexed { index, task ->
                val row = sheet.createRow(index + 1)
                row.createCell(0).setCellValue(task.GetSubjectName())
                row.createCell(1).setCellValue(task.GetDescription().replace("[NEWLINE]", "\n"))
                row.createCell(2).setCellValue(task.GetCreationDate())
                row.createCell(3).setCellValue(task.GetExpireDate())
            }

            // Автоширина колонок
            for (i in 0..3) {
                sheet.setColumnWidth(i, 15 * 256)
            }

            // Сохраняем во временный файл
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "Отчёт_задач_$timeStamp.xlsx"
            val cacheFile = File(context.cacheDir, fileName)
            FileOutputStream(cacheFile).use { workbook.write(it) }
            workbook.close()

            // Возвращаем URI для Android 10+ через FileProvider
            val authority = "${context.packageName}.fileprovider"
            return@withContext FileProvider.getUriForFile(context, authority, cacheFile)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

// Функция для чтения Excel файла и сохранения в Room
suspend fun importTasksFromExcel(context: Context, fileUri: Uri, taskList: TaskList): Boolean {
    return try {
        val inputStream: InputStream? = context.contentResolver.openInputStream(fileUri)
        if (inputStream == null) return false

        // Открываем воркбук с помощью Apache POI (или аналогичной библиотеки, используемой у вас)
        val workbook = WorkbookFactory.create(inputStream)
        val sheet = workbook.getSheetAt(0) ?: return false

        // Начинаем с 1 строки, так как 0-я строка — это заголовки (Предмет, Описание и т.д.)
        for (rowIndex in 1..sheet.lastRowNum) {
            val row = sheet.getRow(rowIndex) ?: continue

            // Читаем ячейки. Приводим к String, обрабатывая пустые значения
            val subject = row.getCell(0)?.toString()?.trim() ?: ""
            val description = row.getCell(1)?.toString()?.trim() ?: ""
            val creationDate = row.getCell(2)?.toString()?.trim() ?: ""
            val expireDate = row.getCell(3)?.toString()?.trim() ?: ""

            // Проверяем валидность строки (как при создании в AddTaskDialog)
            if (subject.isNotBlank() && description.isNotBlank() && creationDate.isNotBlank() && expireDate.isNotBlank()) {
                // Добавляем в список задач (и, соответственно, в БД)
                val realTask = taskList.CreateAndAddNewTaskWithReturn(subject, description, creationDate, expireDate)
                // Сразу планируем уведомление для новой задачи
                NotificationHelper.scheduleNotification(realTask, context)
            }
        }

        workbook.close()
        inputStream.close()
        true // Импорт прошел успешно
    } catch (e: Exception) {
        e.printStackTrace()
        false // Произошла ошибка
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