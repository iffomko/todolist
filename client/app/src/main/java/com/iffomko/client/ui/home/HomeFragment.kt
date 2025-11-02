package com.iffomko.client.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.iffomko.client.databinding.FragmentHomeBinding

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var homeViewModel: HomeViewModel
    private lateinit var todoAdapter: TodoAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        homeViewModel = ViewModelProvider(this)[HomeViewModel::class.java]
        
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root
        
        setupRecyclerView()
        observeViewModel()
        setupFab()
        
        return root
    }
    
    private fun setupFab() {
        binding.fabAddFolder.setOnClickListener {
            todoAdapter.insertNewFolderElement()
        }
    }
    
    private fun setupRecyclerView() {
        todoAdapter = TodoAdapter(
            onTaskClick = { taskId ->
                homeViewModel.toggleTaskCompletion(taskId)
            },
            onFolderClick = { folderId ->
                homeViewModel.toggleFolderExpansion(folderId)
            },
            onNewTaskAdded = { taskTitle, folderId ->
                homeViewModel.addNewTask(taskTitle, folderId)
            },
            onNewSubtaskAdded = { subtaskTitle, folderId, taskId ->
                homeViewModel.addNewSubtask(subtaskTitle, folderId, taskId)
            },
            onNewFolderAdded = { folderTitle ->
                homeViewModel.addNewFolder(folderTitle)
            },
            onTaskTitleUpdated = { taskId, newTitle ->
                homeViewModel.updateTaskTitle(taskId, newTitle)
            },
            onFolderTitleUpdated = { folderId, newTitle ->
                homeViewModel.updateFolderTitle(folderId, newTitle)
            },
            onItemDeleted = { itemId, parentFolderId, parentTaskId ->
                homeViewModel.deleteItem(itemId, parentFolderId, parentTaskId)
            },
            onAddTaskRequested = { folderId ->
                todoAdapter.insertNewTaskElement(folderId)
            },
            onAddSubtaskRequested = { folderId, taskId ->
                todoAdapter.insertNewSubtaskElement(folderId, taskId)
            },
            onRefreshRequested = {
                // Refresh adapter with current ViewModel data
                homeViewModel.todoItems.value?.let { items ->
                    todoAdapter.updateItems(items)
                }
            }
        )
        
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = todoAdapter
        }
    }
    
    private fun observeViewModel() {
        homeViewModel.todoItems.observe(viewLifecycleOwner) { items ->
            todoAdapter.updateItems(items)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}