package com.todolist.server.services

import com.todolist.server.domain.Folder
import com.todolist.server.repositories.FoldersRepository
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import kotlin.jvm.optionals.getOrNull

@Service
class FoldersService(
    private val foldersRepository: FoldersRepository
) {

    fun createFolders(folder: Folder): Folder {
        folder.id = 0
        folder.tasks = mutableListOf()
        return foldersRepository.save(folder)
    }

    fun findFolder(folderId: Long): ResponseEntity<Folder> {
        val folder = foldersRepository.findById(folderId)
        return if (folder.isPresent) ResponseEntity.ok(folder.get())
        else ResponseEntity.notFound().build();
    }

    fun loadFolders(): List<Folder> = foldersRepository.findAll()

    fun updateFolder(folder: Folder): ResponseEntity<Folder> {
        foldersRepository.findById(folder.id).getOrNull()
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(foldersRepository.save(folder))
    }

    fun deleteFolder(folderId: Long): ResponseEntity<Unit> {
        val folder = foldersRepository.findById(folderId).getOrNull()
            ?: return ResponseEntity.notFound().build()
        foldersRepository.deleteById(folder.id)
        return ResponseEntity.ok().build()
    }
}