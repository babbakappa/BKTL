//Файл MainActivity.kt
//По сути самый обычный main, который запускает функцию из UI.kt

package home.babbakappa.bktl
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import android.os.Bundle
import androidx.compose.material3.*

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                TaskListApp()
            }
        }
    }
}
