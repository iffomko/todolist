package com.todolist.server.repositories

import com.todolist.server.domain.Subtask
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface SubtasksRepository : JpaRepository<Subtask, Long>