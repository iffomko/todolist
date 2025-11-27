package com.todolist.server.services

import com.todolist.server.domain.Task
import com.todolist.server.repositories.TasksRepository
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import kotlin.jvm.optionals.getOrNull

@Service
class TasksService(
    private val tasksRepository: TasksRepository,
) {

    fun createTask(task: Task): Task {
        task.id = 0
        task.subtasks = mutableListOf()
        return tasksRepository.save(task)
    }

    fun findTask(taskId: Long): ResponseEntity<Task> {
        val task = tasksRepository.findById(taskId).getOrNull() ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(task)
    }

    fun loadTasks(): List<Task> = tasksRepository.findAll()

    fun updateTask(task: Task): ResponseEntity<Task> {
        tasksRepository.findById(task.id).getOrNull()
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(tasksRepository.save(task))
    }

    fun deleteTask(taskId: Long): ResponseEntity<Unit> {
        val existentTask = tasksRepository.findById(taskId).getOrNull()
            ?: return ResponseEntity.notFound().build()
        tasksRepository.deleteById(existentTask.id)
        return ResponseEntity.ok().build()
    }
}