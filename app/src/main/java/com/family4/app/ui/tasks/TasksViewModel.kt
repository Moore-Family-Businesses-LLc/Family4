package com.family4.app.ui.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.family4.app.data.db.dao.TaskDao
import com.family4.app.data.db.entity.TaskEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TasksViewModel @Inject constructor(
    private val taskDao: TaskDao
) : ViewModel() {

    val activeTasks: StateFlow<List<TaskEntity>> = taskDao.getActiveTasks()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val completedTasks: StateFlow<List<TaskEntity>> = taskDao.getCompletedTasks()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun addTask(title: String, priority: Int = 1, listName: String = "General") {
        viewModelScope.launch {
            taskDao.insertTask(TaskEntity(title = title, priority = priority, listName = listName))
        }
    }

    fun setCompleted(task: TaskEntity, done: Boolean) {
        viewModelScope.launch {
            taskDao.setCompleted(task.id, done, if (done) System.currentTimeMillis() else null)
        }
    }

    fun deleteTask(task: TaskEntity) {
        viewModelScope.launch { taskDao.deleteTask(task) }
    }

    fun updateTask(task: TaskEntity) {
        viewModelScope.launch { taskDao.updateTask(task) }
    }
}
