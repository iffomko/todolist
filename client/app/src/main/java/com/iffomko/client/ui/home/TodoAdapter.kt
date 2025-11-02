package com.iffomko.client.ui.home

import android.view.Gravity
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.iffomko.client.R
import com.iffomko.client.data.TodoItem
import java.text.SimpleDateFormat
import java.util.*

// Data class to hold item with its hierarchy level and parent information
private data class DisplayItem(
    val item: TodoItem,
    val level: Int, // 0 = folder, 1 = task, 2 = subtask, 3 = new_task, 4 = new_subtask
    val parentFolderId: String? = null, // For tasks and subtasks
    val parentTaskId: String? = null // For subtasks only
)

class TodoAdapter(
    private val onTaskClick: (String) -> Unit,
    private val onFolderClick: (String) -> Unit,
    private val onNewTaskAdded: (String, String) -> Unit, // taskTitle, folderId
    private val onNewSubtaskAdded: (String, String, String) -> Unit, // subtaskTitle, folderId, taskId
    private val onNewFolderAdded: (String) -> Unit, // folderTitle
    private val onTaskTitleUpdated: (String, String) -> Unit,
    private val onFolderTitleUpdated: (String, String) -> Unit,
    private val onItemDeleted: (String, String?, String?) -> Unit, // itemId, parentFolderId, parentTaskId
    private val onAddTaskRequested: (String) -> Unit, // folderId - request to add new task editing element
    private val onAddSubtaskRequested: (String, String) -> Unit, // folderId, taskId - request to add new subtask editing element
    private val onRefreshRequested: () -> Unit = {} // Callback to refresh list when temporary item is removed without adding
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var displayItems = listOf<DisplayItem>()
    private val dateFormat = SimpleDateFormat("MMM dd", Locale.getDefault())
    
    // Store temporary editing elements (new_task, new_subtask) that are not in ViewModel
    private val temporaryItems = mutableMapOf<String, DisplayItem>() // key: item.id

    companion object {
        private const val TYPE_FOLDER = 0
        private const val TYPE_TASK = 1
        private const val TYPE_SUBTASK = 2
        private const val TYPE_NEW_TASK = 3
        private const val TYPE_NEW_SUBTASK = 4
        private const val TYPE_NEW_FOLDER = 5
        
        private const val MENU_DELETE = 1
        private const val MENU_ADD = 2
    }

    fun updateItems(newItems: List<TodoItem>) {
        val flatList = mutableListOf<DisplayItem>()
        
        // Check if there's a temporary new_folder element - add it at the beginning
        temporaryItems["new_folder"]?.let { tempItem ->
            flatList.add(tempItem)
        }
        
        newItems.forEach { item ->
            when (item) {
                is TodoItem.Folder -> {
                    flatList.add(DisplayItem(item, 0))
                    if (item.isExpanded) {
                        item.tasks.forEach { task ->
                            flatList.add(DisplayItem(task, 1, parentFolderId = item.id))
                            task.subtasks.forEach { subtask ->
                                flatList.add(DisplayItem(subtask, 2, parentFolderId = item.id, parentTaskId = task.id))
                            }
                            // Check if there's a temporary new_subtask element for this task
                            val tempSubtaskId = "new_subtask_${task.id}"
                            temporaryItems[tempSubtaskId]?.let { tempItem ->
                                flatList.add(tempItem)
                            }
                        }
                        // Check if there's a temporary new_task element for this folder
                        val tempTaskId = "new_task_${item.id}"
                        temporaryItems[tempTaskId]?.let { tempItem ->
                            flatList.add(tempItem)
                        }
                    }
                }
                is TodoItem.Task -> {
                    // Tasks outside folders are not supported in new hierarchy
                }
                else -> {
                    // Other items are not expected in the new structure
                }
            }
        }
        
        displayItems = flatList
        notifyDataSetChanged()
    }
    
    // Method to insert new task editing element after folder
    fun insertNewTaskElement(folderId: String) {
        // Check if already exists
        if (temporaryItems.containsKey("new_task_$folderId")) {
            return
        }
        
        val currentItems = displayItems.toMutableList()
        var folderFound = false
        var folderExpanded = false
        
        // Find the folder and check if it's expanded
        currentItems.forEachIndexed { index, displayItem ->
            if (displayItem.item is TodoItem.Folder && displayItem.item.id == folderId) {
                folderFound = true
                folderExpanded = (displayItem.item as TodoItem.Folder).isExpanded
            }
        }
        
        if (!folderFound) {
            return
        }
        
        // If folder is collapsed, expand it first
        if (!folderExpanded) {
            onFolderClick(folderId)
            // Wait for the folder to expand, then add the element
            // Use postDelayed to give ViewModel time to update
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                insertNewTaskElementInternal(folderId)
            }, 100) // Small delay to allow ViewModel to update
            return
        }
        
        insertNewTaskElementInternal(folderId)
    }
    
    private fun insertNewTaskElementInternal(folderId: String) {
        val currentItems = displayItems.toMutableList()
        var insertIndex = -1
        
        // Find the folder and calculate position after all its tasks
        currentItems.forEachIndexed { index, displayItem ->
            if (displayItem.item is TodoItem.Folder && displayItem.item.id == folderId) {
                // Find the position after all tasks of this folder
                var lastTaskIndex = index
                for (i in index + 1 until currentItems.size) {
                    when {
                        currentItems[i].item is TodoItem.Folder -> {
                            // Next folder found, insert before it
                            insertIndex = i
                            break
                        }
                        currentItems[i].item is TodoItem.Task && currentItems[i].parentFolderId == folderId -> {
                            lastTaskIndex = i
                        }
                        currentItems[i].item is TodoItem.Subtask && currentItems[i].parentFolderId == folderId -> {
                            // Continue - subtasks belong to tasks
                            lastTaskIndex = i
                        }
                    }
                }
                
                if (insertIndex == -1) {
                    // Insert after the last task or after the folder if no tasks
                    insertIndex = lastTaskIndex + 1
                }
            }
        }
        
        if (insertIndex >= 0 && insertIndex <= currentItems.size) {
            val newTaskItem = TodoItem.Task("new_task_$folderId", "Введите название", false)
            val displayItem = DisplayItem(newTaskItem, 3, parentFolderId = folderId)
            // Store in temporary items
            temporaryItems["new_task_$folderId"] = displayItem
            currentItems.add(insertIndex, displayItem)
            displayItems = currentItems
            notifyItemInserted(insertIndex)
        }
    }
    
    // Method to insert new subtask editing element after task
    fun insertNewSubtaskElement(folderId: String, taskId: String) {
        // Check if already exists
        if (temporaryItems.containsKey("new_subtask_$taskId")) {
            return
        }
        
        val currentItems = displayItems.toMutableList()
        var folderFound = false
        var folderExpanded = false
        
        // Find the folder and check if it's expanded
        currentItems.forEachIndexed { index, displayItem ->
            if (displayItem.item is TodoItem.Folder && displayItem.item.id == folderId) {
                folderFound = true
                folderExpanded = (displayItem.item as TodoItem.Folder).isExpanded
            }
        }
        
        if (!folderFound) {
            return
        }
        
        // If folder is collapsed, expand it first
        if (!folderExpanded) {
            onFolderClick(folderId)
            // Wait for the folder to expand, then add the element
            // Use postDelayed to give ViewModel time to update
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                insertNewSubtaskElementInternal(folderId, taskId)
            }, 100) // Small delay to allow ViewModel to update
            return
        }
        
        insertNewSubtaskElementInternal(folderId, taskId)
    }
    
    private fun insertNewSubtaskElementInternal(folderId: String, taskId: String) {
        val currentItems = displayItems.toMutableList()
        var insertIndex = -1
        
        // Find the task and calculate position after all its subtasks
        currentItems.forEachIndexed { index, displayItem ->
            if (displayItem.item is TodoItem.Task && displayItem.item.id == taskId && displayItem.parentFolderId == folderId) {
                // Find the position after all subtasks of this task
                var lastSubtaskIndex = index
                for (i in index + 1 until currentItems.size) {
                    when {
                        currentItems[i].item is TodoItem.Subtask && currentItems[i].parentTaskId == taskId -> {
                            lastSubtaskIndex = i
                        }
                        currentItems[i].item is TodoItem.Task || currentItems[i].item is TodoItem.Folder -> {
                            // Next task or folder found, insert before it
                            insertIndex = i
                            break
                        }
                    }
                }
                
                if (insertIndex == -1) {
                    // Insert after the last subtask or after the task if no subtasks
                    insertIndex = lastSubtaskIndex + 1
                }
            }
        }
        
        if (insertIndex >= 0 && insertIndex <= currentItems.size) {
            val newSubtaskItem = TodoItem.Subtask("new_subtask_$taskId", "Введите название", false)
            val displayItem = DisplayItem(newSubtaskItem, 4, parentFolderId = folderId, parentTaskId = taskId)
            // Store in temporary items
            temporaryItems["new_subtask_$taskId"] = displayItem
            currentItems.add(insertIndex, displayItem)
            displayItems = currentItems
            notifyItemInserted(insertIndex)
        }
    }
    
    // Method to insert new folder editing element at the beginning of the list
    fun insertNewFolderElement() {
        // Check if already exists
        if (temporaryItems.containsKey("new_folder")) {
            return
        }
        
        val currentItems = displayItems.toMutableList()
        val newFolderItem = TodoItem.Folder(
            id = "new_folder",
            title = "Введите название",
            isCompleted = false,
            isExpanded = true,
            tasks = emptyList()
        )
        val displayItem = DisplayItem(newFolderItem, 0)
        
        // Store in temporary items
        temporaryItems["new_folder"] = displayItem
        currentItems.add(0, displayItem)
        displayItems = currentItems
        notifyItemInserted(0)
    }

    override fun getItemViewType(position: Int): Int {
        // Safety check to prevent crashes if list changes
        if (position < 0 || position >= displayItems.size) {
            return TYPE_TASK // Default fallback
        }
        
        val itemId = displayItems[position].item.id
        
        return when {
            displayItems[position].item is TodoItem.Folder -> {
                // Check if it's a temporary editing folder
                if (itemId == "new_folder" || temporaryItems.containsKey(itemId)) {
                    TYPE_NEW_FOLDER
                } else {
                    TYPE_FOLDER
                }
            }
            displayItems[position].item is TodoItem.Task -> {
                if (itemId.startsWith("new_task_") || temporaryItems.containsKey(itemId)) TYPE_NEW_TASK else TYPE_TASK
            }
            displayItems[position].item is TodoItem.Subtask -> {
                if (itemId.startsWith("new_subtask_") || temporaryItems.containsKey(itemId)) TYPE_NEW_SUBTASK else TYPE_SUBTASK
            }
            else -> throw IllegalArgumentException("Unknown item type")
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
            TYPE_NEW_SUBTASK -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_new_task, parent, false)
                NewSubtaskViewHolder(view)
            }
            TYPE_NEW_FOLDER -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_new_folder, parent, false)
                NewFolderViewHolder(view)
            }
            else -> throw IllegalArgumentException("Unknown view type: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        // Safety check to prevent crashes if list changes during binding
        if (position < 0 || position >= displayItems.size) {
            return
        }
        
        val displayItem = displayItems[position]
        val item = displayItem.item
        
        when (holder) {
            is FolderViewHolder -> {
                holder.bind(item as TodoItem.Folder, displayItem.level, displayItem.parentFolderId, position)
            }
            is TaskViewHolder -> {
                holder.bind(item as TodoItem.Task, displayItem.level, displayItem.parentFolderId, position)
            }
            is SubtaskViewHolder -> {
                holder.bind(item as TodoItem.Subtask, displayItem.level, displayItem.parentFolderId, displayItem.parentTaskId, position)
            }
            is NewTaskViewHolder -> {
                holder.bind(displayItem.level, displayItem.parentFolderId ?: "", position)
            }
            is NewSubtaskViewHolder -> {
                holder.bind(displayItem.level, displayItem.parentFolderId ?: "", displayItem.parentTaskId ?: "", position)
            }
            is NewFolderViewHolder -> {
                holder.bind(position)
            }
        }
    }

    override fun getItemCount() = displayItems.size

    private fun showPopupMenu(view: View, folderId: String?, taskId: String?, parentFolderId: String?, isSubtask: Boolean) {
        val popup = PopupMenu(view.context, view, Gravity.END)
        popup.menu.add(0, MENU_DELETE, 0, "Удалить элемент")
        if (!isSubtask) {
            popup.menu.add(0, MENU_ADD, 0, "Добавить элемент")
        }
        
        popup.setOnMenuItemClickListener { item: MenuItem ->
            when (item.itemId) {
                MENU_DELETE -> {
                    onItemDeleted(
                        if (folderId != null) folderId else taskId!!,
                        parentFolderId,
                        if (isSubtask) taskId else null
                    )
                    true
                }
                MENU_ADD -> {
                    if (folderId != null) {
                        // Add task to folder
                        onAddTaskRequested(folderId)
                    } else if (taskId != null && parentFolderId != null) {
                        // Add subtask to task
                        onAddSubtaskRequested(parentFolderId, taskId)
                    }
                    true
                }
                else -> false
            }
        }
        
        popup.show()
    }

    inner class FolderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val folderIcon: android.widget.ImageView = itemView.findViewById(R.id.folder_icon)
        private val folderTitle: TextView = itemView.findViewById(R.id.folder_title)
        private val folderEditText: android.widget.EditText = itemView.findViewById(R.id.folder_edit_text)
        private val expandArrow: android.widget.ImageView = itemView.findViewById(R.id.expand_arrow)
        private val taskCount: TextView = itemView.findViewById(R.id.task_count)
        private val moreButton: android.widget.ImageView = itemView.findViewById(R.id.more_button)

        fun bind(folder: TodoItem.Folder, level: Int, parentFolderId: String?, position: Int) {
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
            
            // Handle more button - show popup menu
            moreButton.setOnClickListener {
                showPopupMenu(moreButton, folder.id, null, null, false)
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
        private val moreButton: android.widget.ImageView = itemView.findViewById(R.id.more_button)

        fun bind(task: TodoItem.Task, level: Int, parentFolderId: String?, position: Int) {
            // Set padding based on level (level 1 = task, should have indentation)
            val rootLayout = itemView as LinearLayout
            val paddingStart = when (level) {
                1 -> itemView.context.resources.getDimensionPixelSize(R.dimen.task_indent)
                else -> itemView.context.resources.getDimensionPixelSize(R.dimen.default_padding)
            }
            val defaultPadding = itemView.context.resources.getDimensionPixelSize(R.dimen.default_padding)
            rootLayout.setPadding(
                paddingStart,
                defaultPadding,
                defaultPadding,
                defaultPadding
            )
            
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
            
            // Handle more button - show popup menu
            moreButton.setOnClickListener {
                showPopupMenu(moreButton, null, task.id, parentFolderId, false)
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
        private val moreButton: android.widget.ImageView = itemView.findViewById(R.id.more_button)

        fun bind(subtask: TodoItem.Subtask, level: Int, parentFolderId: String?, parentTaskId: String?, position: Int) {
            // Set padding based on level (level 2 = subtask, should have more indentation)
            val rootLayout = itemView as LinearLayout
            val paddingStart = when (level) {
                2 -> itemView.context.resources.getDimensionPixelSize(R.dimen.subtask_indent)
                else -> itemView.context.resources.getDimensionPixelSize(R.dimen.default_padding)
            }
            val defaultPadding = itemView.context.resources.getDimensionPixelSize(R.dimen.default_padding)
            rootLayout.setPadding(
                paddingStart,
                defaultPadding,
                defaultPadding,
                defaultPadding
            )
            
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
            
            // Handle more button - show popup menu (only delete for subtasks)
            moreButton.setOnClickListener {
                showPopupMenu(moreButton, null, subtask.id, parentFolderId, true)
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
        private var isProcessing = false // Flag to prevent duplicate submissions

        fun bind(level: Int, folderId: String, position: Int) {
            isProcessing = false // Reset flag when binding
            // New task should have same indentation as regular tasks (level 1)
            val rootLayout = itemView as LinearLayout
            val paddingStart = itemView.context.resources.getDimensionPixelSize(R.dimen.task_indent)
            val defaultPadding = itemView.context.resources.getDimensionPixelSize(R.dimen.default_padding)
            rootLayout.setPadding(
                paddingStart,
                defaultPadding,
                defaultPadding,
                defaultPadding
            )
            
            newTaskEditText.setText("")
            newTaskEditText.hint = "Введите название"
            
            // Auto focus when this item is shown
            newTaskEditText.post {
                newTaskEditText.requestFocus()
                val imm = itemView.context.getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
                imm.showSoftInput(newTaskEditText, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
            }
            
            newTaskEditText.setOnEditorActionListener { _, actionId, _ ->
                if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE || 
                    actionId == android.view.inputmethod.EditorInfo.IME_ACTION_GO) {
                    if (isProcessing) {
                        return@setOnEditorActionListener true // Already processing
                    }
                    
                    isProcessing = true
                    
                    // Hide keyboard first
                    val imm = itemView.context.getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
                    imm.hideSoftInputFromWindow(newTaskEditText.windowToken, 0)
                    
                    // Clear focus to prevent onFocusChangeListener from firing
                    newTaskEditText.clearFocus()
                    
                    val taskText = newTaskEditText.text.toString()
                    val tempKey = "new_task_$folderId"
                    
                    // Remove temporary item before updating ViewModel
                    temporaryItems.remove(tempKey)
                    
                    // Post to next frame to ensure RecyclerView is ready
                    itemView.post {
                        if (taskText.trim().isNotEmpty()) {
                            onNewTaskAdded(taskText.trim(), folderId)
                        } else {
                            // If empty, trigger refresh to remove the editing element
                            onRefreshRequested()
                        }
                    }
                    true
                } else {
                    false
                }
            }
            
            newTaskEditText.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus && !isProcessing) {
                    // Only process if not already processing (to avoid duplicate calls)
                    isProcessing = true
                    
                    val taskText = newTaskEditText.text.toString()
                    val tempKey = "new_task_$folderId"
                    
                    // Post to next frame to avoid conflicts with RecyclerView updates
                    itemView.post {
                        // Remove temporary item - updateItems will handle the UI update
                        temporaryItems.remove(tempKey)
                        if (taskText.trim().isNotEmpty()) {
                            onNewTaskAdded(taskText.trim(), folderId)
                        } else {
                            // If empty, trigger refresh to remove the editing element
                            onRefreshRequested()
                        }
                    }
                }
            }
        }
    }
    
    inner class NewSubtaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val newTaskEditText: android.widget.EditText = itemView.findViewById(R.id.new_task_hint)
        private var isProcessing = false // Flag to prevent duplicate submissions

        fun bind(level: Int, folderId: String, taskId: String, position: Int) {
            isProcessing = false // Reset flag when binding
            // New subtask should have same indentation as regular subtasks (level 2)
            val rootLayout = itemView as LinearLayout
            val paddingStart = itemView.context.resources.getDimensionPixelSize(R.dimen.subtask_indent)
            val defaultPadding = itemView.context.resources.getDimensionPixelSize(R.dimen.default_padding)
            rootLayout.setPadding(
                paddingStart,
                defaultPadding,
                defaultPadding,
                defaultPadding
            )
            
            newTaskEditText.setText("")
            newTaskEditText.hint = "Введите название"
            
            // Auto focus when this item is shown
            newTaskEditText.post {
                newTaskEditText.requestFocus()
                val imm = itemView.context.getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
                imm.showSoftInput(newTaskEditText, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
            }
            
            newTaskEditText.setOnEditorActionListener { _, actionId, _ ->
                if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE || 
                    actionId == android.view.inputmethod.EditorInfo.IME_ACTION_GO) {
                    if (isProcessing) {
                        return@setOnEditorActionListener true // Already processing
                    }
                    
                    isProcessing = true
                    
                    // Hide keyboard first
                    val imm = itemView.context.getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
                    imm.hideSoftInputFromWindow(newTaskEditText.windowToken, 0)
                    
                    // Clear focus to prevent onFocusChangeListener from firing
                    newTaskEditText.clearFocus()
                    
                    val subtaskText = newTaskEditText.text.toString()
                    val tempKey = "new_subtask_$taskId"
                    
                    // Remove temporary item before updating ViewModel
                    temporaryItems.remove(tempKey)
                    
                    // Post to next frame to ensure RecyclerView is ready
                    itemView.post {
                        if (subtaskText.trim().isNotEmpty()) {
                            onNewSubtaskAdded(subtaskText.trim(), folderId, taskId)
                        } else {
                            // If empty, trigger refresh to remove the editing element
                            onRefreshRequested()
                        }
                    }
                    true
                } else {
                    false
                }
            }
            
            newTaskEditText.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus && !isProcessing) {
                    // Only process if not already processing (to avoid duplicate calls)
                    isProcessing = true
                    
                    val subtaskText = newTaskEditText.text.toString()
                    val tempKey = "new_subtask_$taskId"
                    
                    // Post to next frame to avoid conflicts with RecyclerView updates
                    itemView.post {
                        // Remove temporary item - updateItems will handle the UI update
                        temporaryItems.remove(tempKey)
                        if (subtaskText.trim().isNotEmpty()) {
                            onNewSubtaskAdded(subtaskText.trim(), folderId, taskId)
                        } else {
                            // If empty, trigger refresh to remove the editing element
                            onRefreshRequested()
                        }
                    }
                }
            }
        }
    }
    
    inner class NewFolderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val newFolderEditText: android.widget.EditText = itemView.findViewById(R.id.new_folder_hint)
        private var isProcessing = false // Flag to prevent duplicate submissions

        fun bind(position: Int) {
            isProcessing = false // Reset flag when binding
            // New folder should have same indentation as regular folders (level 0)
            val rootLayout = itemView as LinearLayout
            val defaultPadding = itemView.context.resources.getDimensionPixelSize(R.dimen.default_padding)
            rootLayout.setPadding(
                defaultPadding,
                defaultPadding,
                defaultPadding,
                defaultPadding
            )
            
            newFolderEditText.setText("")
            newFolderEditText.hint = "Введите название папки"
            
            // Auto focus when this item is shown
            newFolderEditText.post {
                newFolderEditText.requestFocus()
                val imm = itemView.context.getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
                imm.showSoftInput(newFolderEditText, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
            }
            
            newFolderEditText.setOnEditorActionListener { _, actionId, _ ->
                if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE || 
                    actionId == android.view.inputmethod.EditorInfo.IME_ACTION_GO) {
                    if (isProcessing) {
                        return@setOnEditorActionListener true // Already processing
                    }
                    
                    isProcessing = true
                    
                    // Hide keyboard first
                    val imm = itemView.context.getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
                    imm.hideSoftInputFromWindow(newFolderEditText.windowToken, 0)
                    
                    // Clear focus to prevent onFocusChangeListener from firing
                    newFolderEditText.clearFocus()
                    
                    val folderText = newFolderEditText.text.toString()
                    val tempKey = "new_folder"
                    
                    // Remove temporary item before updating ViewModel
                    temporaryItems.remove(tempKey)
                    
                    // Post to next frame to ensure RecyclerView is ready
                    itemView.post {
                        if (folderText.trim().isNotEmpty()) {
                            onNewFolderAdded(folderText.trim())
                        } else {
                            // If empty, trigger refresh to remove the editing element
                            onRefreshRequested()
                        }
                    }
                    true
                } else {
                    false
                }
            }
            
            newFolderEditText.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus && !isProcessing) {
                    // Only process if not already processing (to avoid duplicate calls)
                    isProcessing = true
                    
                    val folderText = newFolderEditText.text.toString()
                    val tempKey = "new_folder"
                    
                    // Post to next frame to avoid conflicts with RecyclerView updates
                    itemView.post {
                        // Remove temporary item - updateItems will handle the UI update
                        temporaryItems.remove(tempKey)
                        if (folderText.trim().isNotEmpty()) {
                            onNewFolderAdded(folderText.trim())
                        } else {
                            // If empty, trigger refresh to remove the editing element
                            onRefreshRequested()
                        }
                    }
                }
            }
        }
    }
}
