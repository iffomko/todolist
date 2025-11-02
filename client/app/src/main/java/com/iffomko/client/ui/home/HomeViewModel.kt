package com.iffomko.client.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.iffomko.client.data.TodoItem
import java.util.Date

class HomeViewModel(application: Application) : AndroidViewModel(application) {

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
                        dueDate = Date(System.currentTimeMillis() + 2 * 24 * 60 * 60 * 1000),
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
            if (item is TodoItem.Folder) {
                item.copy(tasks = item.tasks.map { task ->
                    when {
                        task.id == taskId -> task.copy(isCompleted = !task.isCompleted)
                        task.subtasks.any { it.id == taskId } -> {
                            task.copy(subtasks = task.subtasks.map { subtask ->
                                if (subtask.id == taskId) {
                                    subtask.copy(isCompleted = !subtask.isCompleted)
                                } else subtask
                            })
                        }
                        else -> task
                    }
                })
            } else item
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

    fun addNewTask(taskTitle: String, folderId: String) {
        if (taskTitle.trim().isEmpty()) return
        
        val currentItems = _todoItems.value ?: return
        
        val newTask = TodoItem.Task(
            id = "task_${System.currentTimeMillis()}",
            title = taskTitle.trim(),
            isCompleted = false
        )
        
        val updatedItems = currentItems.map { item ->
            if (item is TodoItem.Folder && item.id == folderId) {
                item.copy(tasks = item.tasks + newTask)
            } else item
        }
        _todoItems.value = updatedItems
    }
    
    fun addNewSubtask(subtaskTitle: String, folderId: String, taskId: String) {
        if (subtaskTitle.trim().isEmpty()) return
        
        val currentItems = _todoItems.value ?: return
        
        val newSubtask = TodoItem.Subtask(
            id = "subtask_${System.currentTimeMillis()}",
            title = subtaskTitle.trim(),
            isCompleted = false
        )
        
        val updatedItems = currentItems.map { item ->
            if (item is TodoItem.Folder && item.id == folderId) {
                item.copy(tasks = item.tasks.map { task ->
                    if (task.id == taskId) {
                        task.copy(subtasks = task.subtasks + newSubtask)
                    } else task
                })
            } else item
        }
        _todoItems.value = updatedItems
    }
    
    fun addNewFolder(folderTitle: String) {
        if (folderTitle.trim().isEmpty()) return
        
        val newFolder = TodoItem.Folder(
            id = "folder_${System.currentTimeMillis()}",
            title = folderTitle.trim(),
            isCompleted = false,
            isExpanded = false,
            tasks = emptyList()
        )
        
        val currentItems = _todoItems.value ?: emptyList()
        _todoItems.value = currentItems + newFolder
    }

    fun updateTaskTitle(taskId: String, newTitle: String) {
        if (newTitle.trim().isEmpty()) return
        
        val currentItems = _todoItems.value ?: return
        
        val updatedItems = currentItems.map { item ->
            if (item is TodoItem.Folder) {
                item.copy(tasks = item.tasks.map { task ->
                    when {
                        task.id == taskId -> task.copy(title = newTitle.trim())
                        task.subtasks.any { it.id == taskId } -> {
                            task.copy(subtasks = task.subtasks.map { subtask ->
                                if (subtask.id == taskId) {
                                    subtask.copy(title = newTitle.trim())
                                } else subtask
                            })
                        }
                        else -> task
                    }
                })
            } else item
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
    
    fun deleteItem(itemId: String, parentFolderId: String?, parentTaskId: String?) {
        val currentItems = _todoItems.value ?: return
        
        val updatedItems = when {
            parentFolderId == null && parentTaskId == null -> {
                currentItems.filterNot { it is TodoItem.Folder && it.id == itemId }
            }
            parentFolderId != null && parentTaskId == null -> {
                currentItems.map { item ->
                    if (item is TodoItem.Folder && item.id == parentFolderId) {
                        item.copy(tasks = item.tasks.filterNot { it.id == itemId })
                    } else item
                }
            }
            parentFolderId != null && parentTaskId != null -> {
                currentItems.map { item ->
                    if (item is TodoItem.Folder && item.id == parentFolderId) {
                        item.copy(tasks = item.tasks.map { task ->
                            if (task.id == parentTaskId) {
                                task.copy(subtasks = task.subtasks.filterNot { it.id == itemId })
                            } else task
                        })
                    } else item
                }
            }
            else -> currentItems
        }
        
        _todoItems.value = updatedItems
    }
}
