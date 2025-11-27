package com.todolist.server.controllers

import com.todolist.server.domain.Task
import com.todolist.server.domain.tasksUrl
import com.todolist.server.services.TasksService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(tasksUrl)
class TasksController(
    private val tasksService: TasksService
) {
    @PostMapping
    fun createTask(@RequestBody task: Task): Task =
        tasksService.createTask(task)

    @GetMapping("/{taskId}")
    fun findTask(@PathVariable("taskId") taskId: Long): ResponseEntity<Task> =
        tasksService.findTask(taskId)

    @GetMapping
    fun loadTask(): List<Task> = tasksService.loadTasks()

    @PutMapping
    fun updateTask(@RequestBody task: Task): ResponseEntity<Task> =
        tasksService.updateTask(task)

    @DeleteMapping("/{taskId}")
    fun deleteTask(@PathVariable("taskId") taskId: Long) =
        tasksService.deleteTask(taskId)
}