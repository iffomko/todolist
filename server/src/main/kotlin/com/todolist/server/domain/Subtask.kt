package com.todolist.server.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "subtasks")
class Subtask(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long,
    @Column(name = "name", nullable = false)
    var name: String = "",
    @Column(name = "checked", nullable = false)
    var checked: Boolean = false,
    @Column(name = "task_id", nullable = false)
    var taskId: Long
)