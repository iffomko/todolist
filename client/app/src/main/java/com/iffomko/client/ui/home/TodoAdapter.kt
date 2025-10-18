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
    private val onFolderClick: (String) -> Unit
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
        private val expandArrow: ImageView = itemView.findViewById(R.id.expand_arrow)
        private val taskCount: TextView = itemView.findViewById(R.id.task_count)

        fun bind(folder: TodoItem.Folder) {
            folderTitle.text = folder.title
            taskCount.text = folder.tasks.size.toString()
            taskCount.visibility = if (folder.tasks.isNotEmpty()) View.VISIBLE else View.GONE
            
            expandArrow.rotation = if (folder.isExpanded) 0f else -90f
            
            itemView.setOnClickListener {
                onFolderClick(folder.id)
            }
        }
    }

    inner class TaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val taskCheckbox: CheckBox = itemView.findViewById(R.id.task_checkbox)
        private val taskTitle: TextView = itemView.findViewById(R.id.task_title)
        private val dueDateContainer: LinearLayout = itemView.findViewById(R.id.due_date_container)
        private val dueDateText: TextView = itemView.findViewById(R.id.due_date_text)

        fun bind(task: TodoItem.Task) {
            taskTitle.text = task.title
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
            
            taskCheckbox.setOnClickListener {
                onTaskClick(task.id)
            }
            
            itemView.setOnClickListener {
                onTaskClick(task.id)
            }
        }
    }

    inner class SubtaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val subtaskCheckbox: CheckBox = itemView.findViewById(R.id.subtask_checkbox)
        private val subtaskTitle: TextView = itemView.findViewById(R.id.subtask_title)

        fun bind(subtask: TodoItem.Subtask) {
            subtaskTitle.text = subtask.title
            subtaskCheckbox.isChecked = subtask.isCompleted
            
            if (subtask.isCompleted) {
                subtaskTitle.alpha = 0.5f
                subtaskTitle.paintFlags = subtaskTitle.paintFlags or android.graphics.Paint.STRIKE_THRU_TEXT_FLAG
            } else {
                subtaskTitle.alpha = 1.0f
                subtaskTitle.paintFlags = subtaskTitle.paintFlags and android.graphics.Paint.STRIKE_THRU_TEXT_FLAG.inv()
            }
            
            subtaskCheckbox.setOnClickListener {
                onTaskClick(subtask.id)
            }
            
            itemView.setOnClickListener {
                onTaskClick(subtask.id)
            }
        }
    }

    inner class NewTaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val newTaskHint: TextView = itemView.findViewById(R.id.new_task_hint)

        fun bind() {
            itemView.setOnClickListener {
                // Handle new task creation
            }
        }
    }
}
