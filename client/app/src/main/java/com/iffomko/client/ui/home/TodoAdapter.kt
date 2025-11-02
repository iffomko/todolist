package com.iffomko.client.ui.home

import android.os.Handler
import android.os.Looper
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

class TodoAdapter(
    private val onTaskClick: (String) -> Unit,
    private val onFolderClick: (String) -> Unit,
    private val onNewTaskAdded: (String, String) -> Unit,
    private val onNewSubtaskAdded: (String, String, String) -> Unit,
    private val onNewFolderAdded: (String) -> Unit,
    private val onTaskTitleUpdated: (String, String) -> Unit,
    private val onFolderTitleUpdated: (String, String) -> Unit,
    private val onItemDeleted: (String, String?, String?) -> Unit,
    private val onAddTaskRequested: (String) -> Unit,
    private val onAddSubtaskRequested: (String, String) -> Unit,
    private val onRefreshRequested: () -> Unit = {}
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var displayItems = listOf<DisplayItem>()
    private val temporaryItemsManager = TemporaryItemsManager()

    fun updateItems(newItems: List<TodoItem>) {
        val converter = DisplayListConverter(temporaryItemsManager.getAll())
        displayItems = converter.convertToFlatList(newItems)
        notifyDataSetChanged()
    }
    
    fun insertNewTaskElement(folderId: String) {
        val helper = InsertionHelper(displayItems)
        val folderState = helper.findFolderState(folderId) ?: return
        
        if (!folderState.isExpanded) {
            onFolderClick(folderId)
            Handler(Looper.getMainLooper()).postDelayed({
                insertNewTaskElementInternal(folderId)
            }, 100)
            return
        }
        
        insertNewTaskElementInternal(folderId)
    }
    
    private fun insertNewTaskElementInternal(folderId: String) {
        val helper = InsertionHelper(displayItems)
        val insertIndex = helper.findTaskInsertionIndex(folderId) ?: return
        
        val displayItem = temporaryItemsManager.addNewTask(folderId) ?: return
        val currentItems = displayItems.toMutableList()
        
        if (insertIndex <= currentItems.size) {
            currentItems.add(insertIndex, displayItem)
            displayItems = currentItems
            notifyItemInserted(insertIndex)
        }
    }
    
    fun insertNewSubtaskElement(folderId: String, taskId: String) {
        val helper = InsertionHelper(displayItems)
        val folderState = helper.findFolderState(folderId) ?: return
        
        if (!folderState.isExpanded) {
            onFolderClick(folderId)
            Handler(Looper.getMainLooper()).postDelayed({
                insertNewSubtaskElementInternal(folderId, taskId)
            }, 100)
            return
        }
        
        insertNewSubtaskElementInternal(folderId, taskId)
    }
    
    private fun insertNewSubtaskElementInternal(folderId: String, taskId: String) {
        val helper = InsertionHelper(displayItems)
        val insertIndex = helper.findSubtaskInsertionIndex(folderId, taskId) ?: return
        
        val displayItem = temporaryItemsManager.addNewSubtask(folderId, taskId) ?: return
        val currentItems = displayItems.toMutableList()
        
        if (insertIndex <= currentItems.size) {
            currentItems.add(insertIndex, displayItem)
            displayItems = currentItems
            notifyItemInserted(insertIndex)
        }
    }
    
    fun insertNewFolderElement() {
        val helper = InsertionHelper(displayItems)
        val insertIndex = helper.getEndIndex()
        
        val displayItem = temporaryItemsManager.addNewFolder() ?: return
        val currentItems = displayItems.toMutableList()
        currentItems.add(insertIndex, displayItem)
        displayItems = currentItems
        notifyItemInserted(insertIndex)
    }

    override fun getItemViewType(position: Int): Int {
        if (position < 0 || position >= displayItems.size) {
            return TodoAdapterConstants.TYPE_TASK
        }
        
        val itemId = displayItems[position].item.id
        val temporaryItems = temporaryItemsManager.getAll()
        
        return when {
            displayItems[position].item is TodoItem.Folder -> {
                if (itemId == "new_folder" || temporaryItems.containsKey(itemId)) {
                    TodoAdapterConstants.TYPE_NEW_FOLDER
                } else {
                    TodoAdapterConstants.TYPE_FOLDER
                }
            }
            displayItems[position].item is TodoItem.Task -> {
                if (itemId.startsWith("new_task_") || temporaryItems.containsKey(itemId)) {
                    TodoAdapterConstants.TYPE_NEW_TASK
                } else {
                    TodoAdapterConstants.TYPE_TASK
                }
            }
            displayItems[position].item is TodoItem.Subtask -> {
                if (itemId.startsWith("new_subtask_") || temporaryItems.containsKey(itemId)) {
                    TodoAdapterConstants.TYPE_NEW_SUBTASK
                } else {
                    TodoAdapterConstants.TYPE_SUBTASK
                }
            }
            else -> throw IllegalArgumentException("Unknown item type")
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TodoAdapterConstants.TYPE_FOLDER -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_folder, parent, false)
                FolderViewHolder(view)
            }
            TodoAdapterConstants.TYPE_TASK -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_task, parent, false)
                TaskViewHolder(view)
            }
            TodoAdapterConstants.TYPE_SUBTASK -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_subtask, parent, false)
                SubtaskViewHolder(view)
            }
            TodoAdapterConstants.TYPE_NEW_TASK -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_new_task, parent, false)
                NewTaskViewHolder(view)
            }
            TodoAdapterConstants.TYPE_NEW_SUBTASK -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_new_task, parent, false)
                NewSubtaskViewHolder(view)
            }
            TodoAdapterConstants.TYPE_NEW_FOLDER -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_new_folder, parent, false)
                NewFolderViewHolder(view)
            }
            else -> throw IllegalArgumentException("Unknown view type: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
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
        popup.menu.add(0, TodoAdapterConstants.MENU_DELETE, 0, "Удалить элемент")
        if (!isSubtask) {
            popup.menu.add(0, TodoAdapterConstants.MENU_ADD, 0, "Добавить элемент")
        }
        
        popup.setOnMenuItemClickListener { item: MenuItem ->
            when (item.itemId) {
                TodoAdapterConstants.MENU_DELETE -> {
                    onItemDeleted(
                        if (folderId != null) folderId else taskId!!,
                        parentFolderId,
                        if (isSubtask) taskId else null
                    )
                    true
                }
                TodoAdapterConstants.MENU_ADD -> {
                    if (folderId != null) {
                        onAddTaskRequested(folderId)
                    } else if (taskId != null && parentFolderId != null) {
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
            
            itemView.setOnLongClickListener {
                enterEditMode()
                true
            }
            
            itemView.setOnClickListener {
                onFolderClick(folder.id)
            }
            
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
            
            moreButton.setOnClickListener {
                showPopupMenu(moreButton, folder.id, null, null, false)
            }
        }
        
        private fun enterEditMode() {
            folderTitle.visibility = View.GONE
            folderEditText.visibility = View.VISIBLE
            folderEditText.requestFocus()
            folderEditText.selectAll()
            
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
            
            itemView.setOnLongClickListener {
                enterEditMode()
                true
            }
            
            itemView.setOnClickListener {
                onTaskClick(task.id)
            }
            
            taskCheckbox.setOnClickListener {
                onTaskClick(task.id)
            }
            
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
            
            moreButton.setOnClickListener {
                showPopupMenu(moreButton, null, task.id, parentFolderId, false)
            }
        }
        
        private fun enterEditMode() {
            taskTitle.visibility = View.GONE
            taskEditText.visibility = View.VISIBLE
            taskEditText.requestFocus()
            taskEditText.selectAll()
            
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
            
            itemView.setOnLongClickListener {
                enterEditMode()
                true
            }
            
            itemView.setOnClickListener {
                onTaskClick(subtask.id)
            }
            
            subtaskCheckbox.setOnClickListener {
                onTaskClick(subtask.id)
            }
            
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
            
            moreButton.setOnClickListener {
                showPopupMenu(moreButton, null, subtask.id, parentFolderId, true)
            }
        }
        
        private fun enterEditMode() {
            subtaskTitle.visibility = View.GONE
            subtaskEditText.visibility = View.VISIBLE
            subtaskEditText.requestFocus()
            subtaskEditText.selectAll()
            
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
            
            val imm = itemView.context.getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
            imm.hideSoftInputFromWindow(subtaskEditText.windowToken, 0)
        }
    }

    inner class NewTaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val newTaskEditText: android.widget.EditText = itemView.findViewById(R.id.new_task_hint)
        private var isProcessing = false

        fun bind(level: Int, folderId: String, position: Int) {
            isProcessing = false
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
            
            newTaskEditText.post {
                newTaskEditText.requestFocus()
                val imm = itemView.context.getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
                imm.showSoftInput(newTaskEditText, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
            }
            
            newTaskEditText.setOnEditorActionListener { _, actionId, _ ->
                if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE || 
                    actionId == android.view.inputmethod.EditorInfo.IME_ACTION_GO) {
                    if (isProcessing) {
                        return@setOnEditorActionListener true
                    }
                    
                    isProcessing = true
                    
                    val imm = itemView.context.getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
                    imm.hideSoftInputFromWindow(newTaskEditText.windowToken, 0)
                    
                    newTaskEditText.clearFocus()
                    
                    val taskText = newTaskEditText.text.toString()
                    val tempKey = "new_task_$folderId"
                    
                    temporaryItemsManager.remove(tempKey)
                    
                    itemView.post {
                        if (taskText.trim().isNotEmpty()) {
                            onNewTaskAdded(taskText.trim(), folderId)
                        } else {
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
                    isProcessing = true
                    
                    val taskText = newTaskEditText.text.toString()
                    val tempKey = "new_task_$folderId"
                    
                    itemView.post {
                        temporaryItemsManager.remove(tempKey)
                        if (taskText.trim().isNotEmpty()) {
                            onNewTaskAdded(taskText.trim(), folderId)
                        } else {
                            onRefreshRequested()
                        }
                    }
                }
            }
        }
    }
    
    inner class NewSubtaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val newTaskEditText: android.widget.EditText = itemView.findViewById(R.id.new_task_hint)
        private var isProcessing = false

        fun bind(level: Int, folderId: String, taskId: String, position: Int) {
            isProcessing = false
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
            
            newTaskEditText.post {
                newTaskEditText.requestFocus()
                val imm = itemView.context.getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
                imm.showSoftInput(newTaskEditText, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
            }
            
            newTaskEditText.setOnEditorActionListener { _, actionId, _ ->
                if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE || 
                    actionId == android.view.inputmethod.EditorInfo.IME_ACTION_GO) {
                    if (isProcessing) {
                        return@setOnEditorActionListener true
                    }
                    
                    isProcessing = true
                    
                    val imm = itemView.context.getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
                    imm.hideSoftInputFromWindow(newTaskEditText.windowToken, 0)
                    
                    newTaskEditText.clearFocus()
                    
                    val subtaskText = newTaskEditText.text.toString()
                    val tempKey = "new_subtask_$taskId"
                    
                    temporaryItemsManager.remove(tempKey)
                    
                    itemView.post {
                        if (subtaskText.trim().isNotEmpty()) {
                            onNewSubtaskAdded(subtaskText.trim(), folderId, taskId)
                        } else {
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
                    isProcessing = true
                    
                    val subtaskText = newTaskEditText.text.toString()
                    val tempKey = "new_subtask_$taskId"
                    
                    itemView.post {
                        temporaryItemsManager.remove(tempKey)
                        if (subtaskText.trim().isNotEmpty()) {
                            onNewSubtaskAdded(subtaskText.trim(), folderId, taskId)
                        } else {
                            onRefreshRequested()
                        }
                    }
                }
            }
        }
    }
    
    inner class NewFolderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val newFolderEditText: android.widget.EditText = itemView.findViewById(R.id.new_folder_hint)
        private var isProcessing = false

        fun bind(position: Int) {
            isProcessing = false
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
            
            newFolderEditText.post {
                newFolderEditText.requestFocus()
                val imm = itemView.context.getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
                imm.showSoftInput(newFolderEditText, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
            }
            
            newFolderEditText.setOnEditorActionListener { _, actionId, _ ->
                if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE || 
                    actionId == android.view.inputmethod.EditorInfo.IME_ACTION_GO) {
                    if (isProcessing) {
                        return@setOnEditorActionListener true
                    }
                    
                    isProcessing = true
                    
                    val imm = itemView.context.getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
                    imm.hideSoftInputFromWindow(newFolderEditText.windowToken, 0)
                    
                    newFolderEditText.clearFocus()
                    
                    val folderText = newFolderEditText.text.toString()
                    val tempKey = "new_folder"
                    
                    temporaryItemsManager.remove(tempKey)
                    
                    itemView.post {
                        if (folderText.trim().isNotEmpty()) {
                            onNewFolderAdded(folderText.trim())
                        } else {
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
                    isProcessing = true
                    
                    val folderText = newFolderEditText.text.toString()
                    val tempKey = "new_folder"
                    
                    itemView.post {
                        temporaryItemsManager.remove(tempKey)
                        if (folderText.trim().isNotEmpty()) {
                            onNewFolderAdded(folderText.trim())
                        } else {
                            onRefreshRequested()
                        }
                    }
                }
            }
        }
    }
}
