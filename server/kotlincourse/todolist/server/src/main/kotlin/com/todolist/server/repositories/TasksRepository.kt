package com.todolist.server.repositories

import com.todolist.server.domain.Task
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface TasksRepository : JpaRepository<Task, Long>