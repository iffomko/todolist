package com.iffomko.client.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.iffomko.client.data.TodoItem
import java.util.Date

class HomeViewModel : ViewModel() {

    private val _todoItems = MutableLiveData<List<TodoItem>>()
    val todoItems: LiveData<List<TodoItem>> = _todoItems

    init {
        loadSampleData()
    }

    private fun loadSampleData() {
        val sampleData = listOf(
            TodoItem.Folder(
                id = "folder_productivity",
                title = "Productivity",
                isExpanded = true,
                tasks = listOf(
                    TodoItem.Task(
                        id = "task_landing",
                        title = "Work on the landing page",
                        isCompleted = false
                    ),
                    TodoItem.Task(
                        id = "task_food",
                        title = "Buy food",
                        isCompleted = false
                    ),
                    TodoItem.Task(
                        id = "task_banner",
                        title = "design a banner",
                        isCompleted = true
                    ),
                    TodoItem.Task(
                        id = "task_cleaning",
                        title = "Cleaning",
                        isCompleted = false,
                        dueDate = Date(System.currentTimeMillis() + 2 * 24 * 60 * 60 * 1000), // 2 days from now
                        subtasks = listOf(
                            TodoItem.Subtask(
                                id = "subtask_declutter",
                                title = "Declutter phone, laptop",
                                isCompleted = false
                            ),
                            TodoItem.Subtask(
                                id = "subtask_mattress",
                                title = "Get a mattress",
                                isCompleted = false
                            )
                        )
                    ),
                )
            ),
            TodoItem.Folder(
                id = "folder_assignments",
                title = "Assignments",
                isExpanded = false,
                tasks = listOf(
                    TodoItem.Task("task1", "Assignment 1", false),
                    TodoItem.Task("task2", "Assignment 2", false),
                    TodoItem.Task("task3", "Assignment 3", false),
                    TodoItem.Task("task4", "Assignment 4", false),
                    TodoItem.Task("task5", "Assignment 5", false),
                    TodoItem.Task("task6", "Assignment 6", false),
                    TodoItem.Task("task7", "Assignment 7", false),
                    TodoItem.Task("task8", "Assignment 8", false)
                )
            ),
            TodoItem.Folder(
                id = "folder_work",
                title = "Work",
                isExpanded = false,
                tasks = listOf(
                    TodoItem.Task("work1", "Work task 1", false),
                    TodoItem.Task("work2", "Work task 2", false),
                    TodoItem.Task("work3", "Work task 3", false),
                    TodoItem.Task("work4", "Work task 4", false),
                    TodoItem.Task("work5", "Work task 5", false)
                )
            )
        )
        _todoItems.value = sampleData
    }

    fun toggleTaskCompletion(taskId: String) {
        val currentItems = _todoItems.value ?: return
        val updatedItems = currentItems.map { item ->
            when (item) {
                is TodoItem.Task -> {
                    if (item.id == taskId) {
                        item.copy(isCompleted = !item.isCompleted)
                    } else {
                        item.copy(subtasks = item.subtasks.map { subtask ->
                            if (subtask.id == taskId) {
                                subtask.copy(isCompleted = !subtask.isCompleted)
                            } else subtask
                        })
                    }
                }
                is TodoItem.Folder -> {
                    item.copy(tasks = item.tasks.map { task ->
                        if (task.id == taskId) {
                            task.copy(isCompleted = !task.isCompleted)
                        } else {
                            task.copy(subtasks = task.subtasks.map { subtask ->
                                if (subtask.id == taskId) {
                                    subtask.copy(isCompleted = !subtask.isCompleted)
                                } else subtask
                            })
                        }
                    })
                }
                else -> item
            }
        }
        _todoItems.value = updatedItems
    }

    fun toggleFolderExpansion(folderId: String) {
        val currentItems = _todoItems.value ?: return
        val updatedItems = currentItems.map { item ->
            if (item is TodoItem.Folder && item.id == folderId) {
                item.copy(isExpanded = !item.isExpanded)
            } else item
        }
        _todoItems.value = updatedItems
    }

    fun addNewTask(taskTitle: String) {
        if (taskTitle.trim().isEmpty()) return
        
        val currentItems = _todoItems.value ?: return
        val newTask = TodoItem.Task(
            id = "task_${System.currentTimeMillis()}",
            title = taskTitle.trim(),
            isCompleted = false
        )
        
        val updatedItems = currentItems + newTask
        _todoItems.value = updatedItems
    }

    fun updateTaskTitle(taskId: String, newTitle: String) {
        if (newTitle.trim().isEmpty()) return
        
        val currentItems = _todoItems.value ?: return
        val updatedItems = currentItems.map { item ->
            when (item) {
                is TodoItem.Task -> {
                    if (item.id == taskId) {
                        item.copy(title = newTitle.trim())
                    } else {
                        item.copy(subtasks = item.subtasks.map { subtask ->
                            if (subtask.id == taskId) {
                                subtask.copy(title = newTitle.trim())
                            } else subtask
                        })
                    }
                }
                is TodoItem.Folder -> {
                    item.copy(tasks = item.tasks.map { task ->
                        if (task.id == taskId) {
                            task.copy(title = newTitle.trim())
                        } else {
                            task.copy(subtasks = task.subtasks.map { subtask ->
                                if (subtask.id == taskId) {
                                    subtask.copy(title = newTitle.trim())
                                } else subtask
                            })
                        }
                    })
                }
                else -> item
            }
        }
        _todoItems.value = updatedItems
    }

    fun updateFolderTitle(folderId: String, newTitle: String) {
        if (newTitle.trim().isEmpty()) return
        
        val currentItems = _todoItems.value ?: return
        val updatedItems = currentItems.map { item ->
            if (item is TodoItem.Folder && item.id == folderId) {
                item.copy(title = newTitle.trim())
            } else item
        }
        _todoItems.value = updatedItems
    }
}