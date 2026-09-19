//Файл MainActivity.kt
//По сути самый обычный main, который запускает функцию из MainUI.kt

package home.babbakappa.bktl
import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import android.os.Bundle
import androidx.compose.material3.*
import android.os.Build
import android.util.Log
import androidx.core.view.WindowCompat
import kotlinx.coroutines.launch
import java.io.File

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Создание канала уведомлений
        NotificationHelper.createNotificationChannel(this)

        val permissions = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(android.Manifest.permission.POST_NOTIFICATIONS)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(android.Manifest.permission.USE_EXACT_ALARM)
        }
        if (permissions.isNotEmpty()) {
            requestPermissions(permissions.toTypedArray(), 0)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            requestPermissions(arrayOf(android.Manifest.permission.SCHEDULE_EXACT_ALARM), 1)
            Log.d("Notification", "Запрошено разрешение SCHEDULE_EXACT_ALARM")
        }

        migrateLegacyDataIfNeeded()

        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            MaterialTheme {
                TaskListApp()
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        for (i in permissions.indices) {
            val granted = grantResults[i] == PackageManager.PERMISSION_GRANTED
            Log.d("Notification", "Разрешение ${permissions[i]} -> ${if (granted) "ДА" else "НЕТ"}")
        }
    }

    private fun migrateLegacyDataIfNeeded() {
        // Скоуп не блокирует UI
        AppScope.launch {
            val dao = AppDatabase.get(applicationContext).taskDao()

            // Уже что-то есть в Room — выходим, миграция не нужна
            if (dao.hasAnyTask()) return@launch

            // Пути к старым файлам — как в MainUI
            val dataFile    = File(filesDir, DBFilePath).absolutePath
            val archiveFile = File(filesDir, ArchiveFilePath).absolutePath

            val activeTasks   = LoadArrayFromFile(dataFile)
            val archivedTasks = LoadArrayFromFile(archiveFile)

            // Ничего мигрировать не надо
            if (activeTasks.isEmpty() && archivedTasks.isEmpty()) return@launch

            activeTasks.forEach { t ->
                dao.insert(
                    TaskEntity(
                        subjectName  = t.GetSubjectName(),
                        description  = t.GetDescription(),
                        creationDate = t.GetCreationDate(),
                        expireDate   = t.GetExpireDate(),
                        isArchived   = false
                    )
                )
            }
            archivedTasks.forEach { t ->
                dao.insert(
                    TaskEntity(
                        subjectName  = t.GetSubjectName(),
                        description  = t.GetDescription(),
                        creationDate = t.GetCreationDate(),
                        expireDate   = t.GetExpireDate(),
                        isArchived   = true
                    )
                )
            }

            // (опционально) чтобы миграция больше не запускалась —
            // переименуем старые файлы, чтобы понять, что перенос сделан.
            File(filesDir, DBFilePath).let { if (it.exists()) it.renameTo(File(filesDir, "$DBFilePath.migrated")) }
            File(filesDir, ArchiveFilePath).let { if (it.exists()) it.renameTo(File(filesDir, "$ArchiveFilePath.migrated")) }
        }
    }

}
