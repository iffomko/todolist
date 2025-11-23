package com.todolist.server.controllers

import com.todolist.server.domain.Folder
import com.todolist.server.domain.foldersUrl
import com.todolist.server.services.FoldersService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping(foldersUrl)
class FoldersController(
    private val foldersService: FoldersService
) {

    @PostMapping
    fun createFolders(@RequestBody folder: Folder): Folder =
        foldersService.createFolders(folder)

    @GetMapping("/{folderId}")
    fun findFolder(@PathVariable("folderId") folderId: Long): ResponseEntity<Folder> =
        foldersService.findFolder(folderId)

    @GetMapping
    fun loadFolders(): List<Folder> = foldersService.loadFolders()

    @PutMapping
    fun updateFolder(@RequestBody folder: Folder): ResponseEntity<Folder> =
        foldersService.updateFolder(folder)

    @DeleteMapping("/{folderId}")
    fun deleteFolder(@PathVariable("folderId") folderId: Long) =
        foldersService.deleteFolder(folderId)
}