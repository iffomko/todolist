package com.iffomko.client.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.ItemTouchHelper
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
        
        return root
    }
    
    private fun setupRecyclerView() {
        todoAdapter = TodoAdapter(
            onTaskClick = { taskId ->
                homeViewModel.toggleTaskCompletion(taskId)
            },
            onFolderClick = { folderId ->
                homeViewModel.toggleFolderExpansion(folderId)
            },
            onNewTaskAdded = { taskTitle ->
                homeViewModel.addNewTask(taskTitle)
            },
            onTaskTitleUpdated = { taskId, newTitle ->
                homeViewModel.updateTaskTitle(taskId, newTitle)
            },
            onFolderTitleUpdated = { folderId, newTitle ->
                homeViewModel.updateFolderTitle(folderId, newTitle)
            },
            onItemMoved = { fromPosition, toPosition ->
                homeViewModel.moveItem(fromPosition, toPosition)
            }
        )
        
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = todoAdapter
        }
        
        // Setup drag and drop
        val itemTouchHelper = ItemTouchHelper(todoAdapter.itemTouchHelperCallback)
        itemTouchHelper.attachToRecyclerView(binding.recyclerView)
        todoAdapter.setItemTouchHelper(itemTouchHelper)
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