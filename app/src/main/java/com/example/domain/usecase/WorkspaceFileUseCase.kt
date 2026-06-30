package com.example.domain.usecase

import android.net.Uri
import com.example.data.database.PinnedFileEntity
import com.example.data.repository.WorkspaceRepository
import com.example.service.FileManagerService
import com.example.service.FileNode
import kotlinx.coroutines.flow.Flow

class WorkspaceFileUseCase(
    private val fileManager: FileManagerService,
    private val repository: WorkspaceRepository
) {

    val pinnedFilesFlow: Flow<List<PinnedFileEntity>> = repository.pinnedFilesFlow

    suspend fun getPinnedFiles(): List<PinnedFileEntity> = repository.getPinnedFiles()

    suspend fun loadWorkspaceTree(rootUri: Uri): Result<List<FileNode>> = runCatching {
        fileManager.buildTree(rootUri)
    }

    suspend fun readFile(rootUri: Uri, relativePath: String): Result<String> = runCatching {
        fileManager.readFileContent(rootUri, relativePath)
    }

    suspend fun writeFile(
        rootUri: Uri,
        relativePath: String,
        content: String,
        autoSnapshot: Boolean = true,
        snapshotReason: String = "AI Edit"
    ): Result<Boolean> = runCatching {
        if (autoSnapshot) {
            // Read previous content to save as snapshot before overwrite
            val previousContent = fileManager.readFileContent(rootUri, relativePath)
            if (!previousContent.startsWith("Error:")) {
                repository.saveSnapshot(relativePath, previousContent, snapshotReason)
            }
        }
        fileManager.writeFileContent(rootUri, relativePath, content)
    }

    suspend fun deleteFile(rootUri: Uri, relativePath: String): Result<Boolean> = runCatching {
        fileManager.deleteFile(rootUri, relativePath)
    }

    suspend fun createNewFile(rootUri: Uri, relativePath: String, initialContent: String = ""): Result<Boolean> = runCatching {
        fileManager.writeFileContent(rootUri, relativePath, initialContent)
    }

    suspend fun pinFile(filePath: String, displayName: String, projectUri: String): Result<Unit> = runCatching {
        repository.pinFile(filePath, displayName, projectUri)
    }

    suspend fun unpinFile(filePath: String): Result<Unit> = runCatching {
        repository.unpinFile(filePath)
    }

    fun getSnapshotsForFile(filePath: String) = repository.getSnapshotsFlow(filePath)

    suspend fun rollbackSnapshot(rootUri: Uri, snapshotId: Int, filePath: String, content: String): Result<Boolean> = runCatching {
        val success = fileManager.writeFileContent(rootUri, filePath, content)
        if (success) {
            repository.deleteSnapshot(snapshotId)
        }
        success
    }

    suspend fun searchInProject(rootUri: Uri, query: String): Result<List<String>> = runCatching {
        fileManager.searchInProject(rootUri, query)
    }
}
