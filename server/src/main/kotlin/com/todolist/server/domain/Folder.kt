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
@Table(name = "folders")
data class Folder(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long,
    @Column(name = "name", nullable = false)
    var name: String,
    @Column(name = "expanded", nullable = false)
    var expanded: Boolean,
    @OneToMany(mappedBy="folderId", fetch = FetchType.EAGER, cascade = [CascadeType.REMOVE])
    var tasks: MutableList<Task> = mutableListOf(),
)