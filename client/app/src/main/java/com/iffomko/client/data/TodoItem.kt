package com.iffomko.client.data

import java.util.Date

sealed class TodoItem {
    abstract val id: String
    abstract val title: String
    abstract val isCompleted: Boolean
    
    data class Task(
        override val id: String,
        override val title: String,
        override val isCompleted: Boolean,
        val dueDate: Date? = null,
        val subtasks: List<Subtask> = emptyList()
    ) : TodoItem()
    
    data class Subtask(
        override val id: String,
        override val title: String,
        override val isCompleted: Boolean
    ) : TodoItem()
    
    data class Folder(
        override val id: String,
        override val title: String,
        override val isCompleted: Boolean = false,
        val isExpanded: Boolean = true,
        val tasks: List<Task> = emptyList()
    ) : TodoItem()
}
