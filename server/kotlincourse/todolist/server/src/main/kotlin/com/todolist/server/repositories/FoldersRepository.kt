package com.todolist.server.repositories

import com.todolist.server.domain.Folder
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface FoldersRepository : JpaRepository<Folder, Long>