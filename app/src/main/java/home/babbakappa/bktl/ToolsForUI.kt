//Файл ToolsForUI.kt
//Функции этого файла используются в UI.kt, нужны для выполнения
//некоторых функций, таких как определения разницы между датами или
//сохранения отчета в Excel

package home.babbakappa.bktl

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.poi.ss.usermodel.*
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.ui.graphics.Color

//Функция для определения цвета чтобы подсветить дату дедлайна
fun getDeadLineColor(other_date: String, pattern: String = "dd.MM.yyyy"): Color {
    return try {
        val formatter = DateTimeFormatter.ofPattern(pattern)
        val today = LocalDate.now()
        val deadline = LocalDate.parse(other_date, formatter)

        val daysLeft = ChronoUnit.DAYS.between(today, deadline)

        when {
            daysLeft < 0 -> Color.Black //Просрочено совсем
            daysLeft <= 3 -> Color.Red //Горит (0-3 дня)
            daysLeft <= 5 -> Color(0xFFFFA500) //Предупреждение (4-5 дней)
            else -> Color.White
        }
    } catch (error: Exception) {
        Color.Gray
    }
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