//Файл MainActivity.kt
//По сути самый обычный main, который запускает функцию из UI.kt

package home.babbakappa.bktl
import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import android.os.Bundle
import androidx.compose.material3.*
import android.os.Build
import android.util.Log

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
}
