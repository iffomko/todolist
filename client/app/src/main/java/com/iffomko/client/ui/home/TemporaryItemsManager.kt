package com.iffomko.client.ui.home

import com.iffomko.client.data.TodoItem

class TemporaryItemsManager {
    
    private val temporaryItems = mutableMapOf<String, DisplayItem>()
    
    fun addNewTask(folderId: String): DisplayItem? {
        val key = "new_task_$folderId"
        if (temporaryItems.containsKey(key)) return null
        
        val newTaskItem = TodoItem.Task("new_task_$folderId", "Введите название", false)
        val displayItem = DisplayItem(newTaskItem, 3, parentFolderId = folderId)
        temporaryItems[key] = displayItem
        return displayItem
    }
    
    fun addNewSubtask(folderId: String, taskId: String): DisplayItem? {
        val key = "new_subtask_$taskId"
        if (temporaryItems.containsKey(key)) return null
        
        val newSubtaskItem = TodoItem.Subtask("new_subtask_$taskId", "Введите название", false)
        val displayItem = DisplayItem(newSubtaskItem, 4, parentFolderId = folderId, parentTaskId = taskId)
        temporaryItems[key] = displayItem
        return displayItem
    }
    
    fun addNewFolder(): DisplayItem? {
        val key = "new_folder"
        if (temporaryItems.containsKey(key)) return null
        
        val newFolderItem = TodoItem.Folder(
            id = "new_folder",
            title = "Введите название",
            isCompleted = false,
            isExpanded = true,
            tasks = emptyList()
        )
        val displayItem = DisplayItem(newFolderItem, 0)
        temporaryItems[key] = displayItem
        return displayItem
    }
    
    fun remove(key: String) {
        temporaryItems.remove(key)
    }
    
    fun contains(key: String): Boolean = temporaryItems.containsKey(key)
    
    fun getAll(): Map<String, DisplayItem> = temporaryItems.toMap()
}

