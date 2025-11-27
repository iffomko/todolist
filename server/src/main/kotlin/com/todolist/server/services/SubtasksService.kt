package com.todolist.server.services

import com.todolist.server.domain.Subtask
import com.todolist.server.repositories.SubtasksRepository
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import kotlin.jvm.optionals.getOrNull

@Service
class SubtasksService(
    private val subtasksRepository: SubtasksRepository
) {

    fun createSubtask(subtask: Subtask): Subtask {
        subtask.id = 0
        return subtasksRepository.save(subtask)
    }

    fun findSubtask(subtaskId: Long): ResponseEntity<Subtask> {
        val subtask = subtasksRepository.findById(subtaskId).getOrNull() ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(subtask)
    }

    fun loadSubtasks(): List<Subtask> = subtasksRepository.findAll()

    fun updateSubtask(subtask: Subtask): ResponseEntity<Subtask> {
        subtasksRepository.findById(subtask.id).getOrNull()
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(subtasksRepository.save(subtask))
    }

    fun deleteSubtask(subtaskId: Long): ResponseEntity<Unit> {
        val existentSubtask = subtasksRepository.findById(subtaskId).getOrNull()
            ?: return ResponseEntity.notFound().build()
        subtasksRepository.deleteById(existentSubtask.id)
        return ResponseEntity.ok().build()
    }
}