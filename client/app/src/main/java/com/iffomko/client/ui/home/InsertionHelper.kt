package com.iffomko.client.ui.home

import com.iffomko.client.data.TodoItem

class InsertionHelper(
    private val displayItems: List<DisplayItem>
) {
    
    fun findTaskInsertionIndex(folderId: String): Int? {
        var folderIndex = -1
        var lastTaskIndex = -1
        
        displayItems.forEachIndexed { index, displayItem ->
            when {
                displayItem.item is TodoItem.Folder && displayItem.item.id == folderId -> {
                    folderIndex = index
                    lastTaskIndex = index
                }
                folderIndex >= 0 && displayItem.item is TodoItem.Folder -> {
                    return index
                }
                folderIndex >= 0 && displayItem.item is TodoItem.Task && displayItem.parentFolderId == folderId -> {
                    lastTaskIndex = index
                }
                folderIndex >= 0 && displayItem.item is TodoItem.Subtask && displayItem.parentFolderId == folderId -> {
                    lastTaskIndex = index
                }
            }
        }
        
        return if (folderIndex >= 0) lastTaskIndex + 1 else null
    }
    
    fun findSubtaskInsertionIndex(folderId: String, taskId: String): Int? {
        var taskIndex = -1
        var lastSubtaskIndex = -1
        
        displayItems.forEachIndexed { index, displayItem ->
            when {
                displayItem.item is TodoItem.Task && displayItem.item.id == taskId && displayItem.parentFolderId == folderId -> {
                    taskIndex = index
                    lastSubtaskIndex = index
                }
                taskIndex >= 0 && displayItem.item is TodoItem.Subtask && displayItem.parentTaskId == taskId -> {
                    lastSubtaskIndex = index
                }
                taskIndex >= 0 && (displayItem.item is TodoItem.Task || displayItem.item is TodoItem.Folder) -> {
                    return index
                }
            }
        }
        
        return if (taskIndex >= 0) lastSubtaskIndex + 1 else null
    }
    
    fun findFolderState(folderId: String): FolderState? {
        displayItems.forEach { displayItem ->
            if (displayItem.item is TodoItem.Folder && displayItem.item.id == folderId) {
                return FolderState(
                    found = true,
                    isExpanded = displayItem.item.isExpanded
                )
            }
        }
        return null
    }
    
    fun getEndIndex(): Int = displayItems.size
}

data class FolderState(
    val found: Boolean,
    val isExpanded: Boolean
)

