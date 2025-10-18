package com.iffomko.client.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.iffomko.client.R
import com.iffomko.client.data.TodoItem
import java.text.SimpleDateFormat
import java.util.*

class TodoAdapter(
    private val onTaskClick: (String) -> Unit,
    private val onFolderClick: (String) -> Unit,
    private val onNewTaskAdded: (String) -> Unit,
    private val onTaskTitleUpdated: (String, String) -> Unit,
    private val onFolderTitleUpdated: (String, String) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var items = listOf<TodoItem>()
    private val dateFormat = SimpleDateFormat("MMM dd", Locale.getDefault())

    companion object {
        private const val TYPE_FOLDER = 0
        private const val TYPE_TASK = 1
        private const val TYPE_SUBTASK = 2
        private const val TYPE_NEW_TASK = 3
    }

    fun updateItems(newItems: List<TodoItem>) {
        val flatList = mutableListOf<TodoItem>()
        
        newItems.forEach { item ->
            when (item) {
                is TodoItem.Folder -> {
                    flatList.add(item)
                    if (item.isExpanded) {
                        item.tasks.forEach { task ->
                            flatList.add(task)
                            task.subtasks.forEach { subtask ->
                                flatList.add(subtask)
                            }
                        }
                    }
                }
                is TodoItem.Task -> {
                    flatList.add(item)
                    item.subtasks.forEach { subtask ->
                        flatList.add(subtask)
                    }
                }
                else -> flatList.add(item)
            }
        }
        
        // Add new task item at the end
        flatList.add(TodoItem.Task("new_task", "Write a task...", false))
        
        items = flatList
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is TodoItem.Folder -> TYPE_FOLDER
            is TodoItem.Task -> if (items[position].id == "new_task") TYPE_NEW_TASK else TYPE_TASK
            is TodoItem.Subtask -> TYPE_SUBTASK
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_FOLDER -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_folder, parent, false)
                FolderViewHolder(view)
            }
            TYPE_TASK -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_task, parent, false)
                TaskViewHolder(view)
            }
            TYPE_SUBTASK -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_subtask, parent, false)
                SubtaskViewHolder(view)
            }
            TYPE_NEW_TASK -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_new_task, parent, false)
                NewTaskViewHolder(view)
            }
            else -> throw IllegalArgumentException("Unknown view type: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = items[position]
        
        when (holder) {
            is FolderViewHolder -> holder.bind(item as TodoItem.Folder)
            is TaskViewHolder -> holder.bind(item as TodoItem.Task)
            is SubtaskViewHolder -> holder.bind(item as TodoItem.Subtask)
            is NewTaskViewHolder -> holder.bind()
        }
    }

    override fun getItemCount() = items.size

    inner class FolderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val folderIcon: ImageView = itemView.findViewById(R.id.folder_icon)
        private val folderTitle: TextView = itemView.findViewById(R.id.folder_title)
        private val folderEditText: android.widget.EditText = itemView.findViewById(R.id.folder_edit_text)
        private val expandArrow: ImageView = itemView.findViewById(R.id.expand_arrow)
        private val taskCount: TextView = itemView.findViewById(R.id.task_count)

        fun bind(folder: TodoItem.Folder) {
            folderTitle.text = folder.title
            folderEditText.setText(folder.title)
            taskCount.text = folder.tasks.size.toString()
            taskCount.visibility = if (folder.tasks.isNotEmpty()) View.VISIBLE else View.GONE
            
            expandArrow.rotation = if (folder.isExpanded) 0f else -90f
            
            // Switch to edit mode on long click
            itemView.setOnLongClickListener {
                enterEditMode()
                true
            }
            
            // Handle single click for expand/collapse
            itemView.setOnClickListener {
                onFolderClick(folder.id)
            }
            
            // Handle edit text
            folderEditText.setOnEditorActionListener { _, actionId, _ ->
                if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE) {
                    exitEditMode(folder.id)
                    true
                } else false
            }
            
            folderEditText.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) {
                    exitEditMode(folder.id)
                }
            }
        }
        
        private fun enterEditMode() {
            folderTitle.visibility = View.GONE
            folderEditText.visibility = View.VISIBLE
            folderEditText.requestFocus()
            folderEditText.selectAll()
            
            // Show keyboard
            val imm = itemView.context.getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
            imm.showSoftInput(folderEditText, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
        }
        
        private fun exitEditMode(folderId: String) {
            val newTitle = folderEditText.text.toString()
            if (newTitle.trim().isNotEmpty()) {
                onFolderTitleUpdated(folderId, newTitle)
            }
            
            folderTitle.visibility = View.VISIBLE
            folderEditText.visibility = View.GONE
            
            // Hide keyboard
            val imm = itemView.context.getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
            imm.hideSoftInputFromWindow(folderEditText.windowToken, 0)
        }
    }

    inner class TaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val taskCheckbox: CheckBox = itemView.findViewById(R.id.task_checkbox)
        private val taskTitle: TextView = itemView.findViewById(R.id.task_title)
        private val taskEditText: android.widget.EditText = itemView.findViewById(R.id.task_edit_text)
        private val dueDateContainer: LinearLayout = itemView.findViewById(R.id.due_date_container)
        private val dueDateText: TextView = itemView.findViewById(R.id.due_date_text)

        fun bind(task: TodoItem.Task) {
            taskTitle.text = task.title
            taskEditText.setText(task.title)
            taskCheckbox.isChecked = task.isCompleted
            
            if (task.isCompleted) {
                taskTitle.alpha = 0.5f
                taskTitle.paintFlags = taskTitle.paintFlags or android.graphics.Paint.STRIKE_THRU_TEXT_FLAG
            } else {
                taskTitle.alpha = 1.0f
                taskTitle.paintFlags = taskTitle.paintFlags and android.graphics.Paint.STRIKE_THRU_TEXT_FLAG.inv()
            }
            
            if (task.dueDate != null) {
                dueDateContainer.visibility = View.VISIBLE
                val daysUntilDue = ((task.dueDate.time - System.currentTimeMillis()) / (1000 * 60 * 60 * 24)).toInt()
                dueDateText.text = "Due in $daysUntilDue days"
            } else {
                dueDateContainer.visibility = View.GONE
            }
            
            // Switch to edit mode on long click
            itemView.setOnLongClickListener {
                enterEditMode()
                true
            }
            
            // Handle single click for completion toggle
            itemView.setOnClickListener {
                onTaskClick(task.id)
            }
            
            // Handle checkbox click
            taskCheckbox.setOnClickListener {
                onTaskClick(task.id)
            }
            
            // Handle edit text
            taskEditText.setOnEditorActionListener { _, actionId, _ ->
                if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE) {
                    exitEditMode(task.id)
                    true
                } else false
            }
            
            taskEditText.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) {
                    exitEditMode(task.id)
                }
            }
        }
        
        private fun enterEditMode() {
            taskTitle.visibility = View.GONE
            taskEditText.visibility = View.VISIBLE
            taskEditText.requestFocus()
            taskEditText.selectAll()
            
            // Show keyboard
            val imm = itemView.context.getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
            imm.showSoftInput(taskEditText, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
        }
        
        private fun exitEditMode(taskId: String) {
            val newTitle = taskEditText.text.toString()
            if (newTitle.trim().isNotEmpty()) {
                onTaskTitleUpdated(taskId, newTitle)
            }
            
            taskTitle.visibility = View.VISIBLE
            taskEditText.visibility = View.GONE
            
            // Hide keyboard
            val imm = itemView.context.getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
            imm.hideSoftInputFromWindow(taskEditText.windowToken, 0)
        }
    }

    inner class SubtaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val subtaskCheckbox: CheckBox = itemView.findViewById(R.id.subtask_checkbox)
        private val subtaskTitle: TextView = itemView.findViewById(R.id.subtask_title)
        private val subtaskEditText: android.widget.EditText = itemView.findViewById(R.id.subtask_edit_text)

        fun bind(subtask: TodoItem.Subtask) {
            subtaskTitle.text = subtask.title
            subtaskEditText.setText(subtask.title)
            subtaskCheckbox.isChecked = subtask.isCompleted
            
            if (subtask.isCompleted) {
                subtaskTitle.alpha = 0.5f
                subtaskTitle.paintFlags = subtaskTitle.paintFlags or android.graphics.Paint.STRIKE_THRU_TEXT_FLAG
            } else {
                subtaskTitle.alpha = 1.0f
                subtaskTitle.paintFlags = subtaskTitle.paintFlags and android.graphics.Paint.STRIKE_THRU_TEXT_FLAG.inv()
            }
            
            // Switch to edit mode on long click
            itemView.setOnLongClickListener {
                enterEditMode()
                true
            }
            
            // Handle single click for completion toggle
            itemView.setOnClickListener {
                onTaskClick(subtask.id)
            }
            
            subtaskCheckbox.setOnClickListener {
                onTaskClick(subtask.id)
            }
            
            // Handle edit text
            subtaskEditText.setOnEditorActionListener { _, actionId, _ ->
                if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE) {
                    exitEditMode(subtask.id)
                    true
                } else false
            }
            
            subtaskEditText.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) {
                    exitEditMode(subtask.id)
                }
            }
        }
        
        private fun enterEditMode() {
            subtaskTitle.visibility = View.GONE
            subtaskEditText.visibility = View.VISIBLE
            subtaskEditText.requestFocus()
            subtaskEditText.selectAll()
            
            // Show keyboard
            val imm = itemView.context.getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
            imm.showSoftInput(subtaskEditText, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
        }
        
        private fun exitEditMode(subtaskId: String) {
            val newTitle = subtaskEditText.text.toString()
            if (newTitle.trim().isNotEmpty()) {
                onTaskTitleUpdated(subtaskId, newTitle)
            }
            
            subtaskTitle.visibility = View.VISIBLE
            subtaskEditText.visibility = View.GONE
            
            // Hide keyboard
            val imm = itemView.context.getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
            imm.hideSoftInputFromWindow(subtaskEditText.windowToken, 0)
        }
    }

    inner class NewTaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val newTaskEditText: android.widget.EditText = itemView.findViewById(R.id.new_task_hint)

        fun bind() {
            newTaskEditText.setText("")
            newTaskEditText.hint = "Write a task..."
            
            newTaskEditText.setOnEditorActionListener { _, actionId, _ ->
                if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE || 
                    actionId == android.view.inputmethod.EditorInfo.IME_ACTION_GO) {
                    val taskText = newTaskEditText.text.toString()
                    if (taskText.trim().isNotEmpty()) {
                        onNewTaskAdded(taskText)
                        newTaskEditText.setText("")
                    }
                    true
                } else {
                    false
                }
            }
            
            newTaskEditText.setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) {
                    newTaskEditText.hint = ""
                } else {
                    newTaskEditText.hint = "Write a task..."
                }
            }
            
            itemView.setOnClickListener {
                newTaskEditText.requestFocus()
                // Show keyboard
                val imm = itemView.context.getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
                imm.showSoftInput(newTaskEditText, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
            }
        }
    }
}
