package com.iffomko.client.ui.home

import com.iffomko.client.data.TodoItem

data class DisplayItem(
    val item: TodoItem,
    val level: Int,
    val parentFolderId: String? = null,
    val parentTaskId: String? = null
)

class DisplayListConverter(
    private val temporaryItems: Map<String, DisplayItem>
) {
    
    fun convertToFlatList(items: List<TodoItem>): List<DisplayItem> {
        val flatList = mutableListOf<DisplayItem>()
        
        items.forEach { item ->
            when (item) {
                is TodoItem.Folder -> {
                    flatList.add(DisplayItem(item, 0))
                    if (item.isExpanded) {
                        addFolderContent(item, flatList)
                    }
                }
                else -> {}
            }
        }
        
        temporaryItems["new_folder"]?.let { tempItem ->
            flatList.add(tempItem)
        }
        
        return flatList
    }
    
    private fun addFolderContent(folder: TodoItem.Folder, flatList: MutableList<DisplayItem>) {
        folder.tasks.forEach { task ->
            flatList.add(DisplayItem(task, 1, parentFolderId = folder.id))
            task.subtasks.forEach { subtask ->
                flatList.add(DisplayItem(subtask, 2, parentFolderId = folder.id, parentTaskId = task.id))
            }
            
            val tempSubtaskId = "new_subtask_${task.id}"
            temporaryItems[tempSubtaskId]?.let { tempItem ->
                flatList.add(tempItem)
            }
        }
        
        val tempTaskId = "new_task_${folder.id}"
        temporaryItems[tempTaskId]?.let { tempItem ->
            flatList.add(tempItem)
        }
    }
}

