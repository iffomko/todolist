package com.todolist.server.controllers

import com.todolist.server.domain.Subtask
import com.todolist.server.domain.subtasksUrl
import com.todolist.server.services.SubtasksService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping(subtasksUrl)
class SubtasksController(
    private val subtasksService: SubtasksService
) {
    @PostMapping
    fun createSubtask(@RequestBody subtask: Subtask): Subtask =
        subtasksService.createSubtask(subtask)

    @GetMapping("/{subtaskId}")
    fun findSubtask(@PathVariable("subtaskId") subtaskId: Long): ResponseEntity<Subtask> =
        subtasksService.findSubtask(subtaskId)

    @GetMapping
    fun loadSubtask(): List<Subtask> = subtasksService.loadSubtasks()

    @PutMapping
    fun updateSubtask(@RequestBody subtask: Subtask): ResponseEntity<Subtask> =
        subtasksService.updateSubtask(subtask)

    @DeleteMapping("/{subtaskId}")
    fun deleteSubtask(@PathVariable("subtaskId") subtaskId: Long) =
        subtasksService.deleteSubtask(subtaskId)
}