package com.todolist.server.domain

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.Table

@Entity
@Table(name = "tasks")
data class Task(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long,
    @Column(name = "folder_id", nullable = false)
    var folderId: Long,
    @Column(name = "name", nullable = false)
    var name: String = "",
    @Column(name = "checked", nullable = false)
    var checked: Boolean = false,
    @OneToMany(mappedBy = "taskId", fetch = FetchType.EAGER, cascade = [CascadeType.REMOVE])
    var subtasks: MutableList<Subtask> = mutableListOf(),
)