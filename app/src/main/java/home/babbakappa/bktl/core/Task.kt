/* Task.kt
* Дата класс для объекта Task */

package home.babbakappa.bktl.core


data class Task(var subject_name: String,
                var description: String,
                var creation_date: String,
                var expire_date: String,
                var id: Long = 0)