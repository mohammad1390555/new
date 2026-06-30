package com.example.service

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream

data class FileNode(
    val name: String,
    val relativePath: String,
    val isDirectory: Boolean,
    val size: Long = 0L,
    val children: List<FileNode> = emptyList()
)

class FileManagerService(private val context: Context) {

    // Set of ignore patterns similar to .gitignore
    private val ignorePatterns = setOf(
        "node_modules", ".git", ".gradle", "build", "dist", ".DS_Store", "bin", "obj"
    )

    private fun shouldIgnore(name: String): Boolean {
        return ignorePatterns.any { name.equals(it, ignoreCase = true) || name.startsWith(".") }
    }

    /**
     * Builds a collapsible tree of the workspace from a SAF Uri
     */
    suspend fun buildTree(rootUri: Uri): List<FileNode> = withContext(Dispatchers.IO) {
        val rootDoc = DocumentFile.fromTreeUri(context, rootUri) ?: return@withContext emptyList()
        return@withContext traverseDirectory(rootDoc, "")
    }

    private fun traverseDirectory(dir: DocumentFile, currentPath: String): List<FileNode> {
        val result = mutableListOf<FileNode>()
        val files = dir.listFiles()
        for (file in files) {
            val name = file.name ?: continue
            if (shouldIgnore(name)) continue

            val relativePath = if (currentPath.isEmpty()) name else "$currentPath/$name"
            if (file.isDirectory) {
                val children = traverseDirectory(file, relativePath)
                result.add(
                    FileNode(
                        name = name,
                        relativePath = relativePath,
                        isDirectory = true,
                        children = children.sortedWith(compareBy({ !it.isDirectory }, { it.name }))
                    )
                )
            } else {
                result.add(
                    FileNode(
                        name = name,
                        relativePath = relativePath,
                        isDirectory = false,
                        size = file.length()
                    )
                )
            }
        }
        return result.sortedWith(compareBy({ !it.isDirectory }, { it.name }))
    }

    /**
     * Finds a DocumentFile by relative path (e.g. "src/main/java/MainActivity.kt")
     */
    suspend fun findFile(rootUri: Uri, relativePath: String): DocumentFile? = withContext(Dispatchers.IO) {
        val rootDoc = DocumentFile.fromTreeUri(context, rootUri) ?: return@withContext null
        if (relativePath.isEmpty()) return@withContext rootDoc

        val parts = relativePath.split("/")
        var current: DocumentFile = rootDoc
        for (part in parts) {
            if (part.isEmpty()) continue
            current = current.findFile(part) ?: return@withContext null
        }
        return@withContext current
    }

    /**
     * Reads file content as String
     */
    suspend fun readFileContent(rootUri: Uri, relativePath: String): String = withContext(Dispatchers.IO) {
        val docFile = findFile(rootUri, relativePath) ?: return@withContext "Error: File not found at $relativePath"
        if (docFile.isDirectory) return@withContext "Error: Path is a directory"

        val inputStream = context.contentResolver.openInputStream(docFile.uri)
            ?: return@withContext "Error: Could not open stream"
        
        return@withContext inputStream.bufferedReader().use { it.readText() }
    }

    /**
     * Writes/Overwrites a file with string content
     */
    suspend fun writeFileContent(rootUri: Uri, relativePath: String, content: String): Boolean = withContext(Dispatchers.IO) {
        try {
            var docFile = findFile(rootUri, relativePath)
            if (docFile == null) {
                // File does not exist, let's create it. We need to find or create parent directories.
                val parts = relativePath.split("/")
                val fileName = parts.last()
                val parentPath = parts.dropLast(1).joinToString("/")

                val parentDoc = if (parentPath.isEmpty()) {
                    DocumentFile.fromTreeUri(context, rootUri)
                } else {
                    findOrCreateDirectory(rootUri, parentPath)
                }

                if (parentDoc != null) {
                    docFile = parentDoc.createFile("text/plain", fileName)
                }
            }

            if (docFile == null) return@withContext false

            val outputStream: OutputStream = context.contentResolver.openOutputStream(docFile.uri, "rwt")
                ?: return@withContext false
            outputStream.use { it.write(content.toByteArray(Charsets.UTF_8)) }
            return@withContext true
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext false
        }
    }

    private fun findOrCreateDirectory(rootUri: Uri, relativePath: String): DocumentFile? {
        val rootDoc = DocumentFile.fromTreeUri(context, rootUri) ?: return null
        val parts = relativePath.split("/")
        var current: DocumentFile = rootDoc
        for (part in parts) {
            if (part.isEmpty()) continue
            var next = current.findFile(part)
            if (next == null || !next.isDirectory) {
                next = current.createDirectory(part) ?: return null
            }
            current = next
        }
        return current
    }

    /**
     * Deletes a file or directory
     */
    suspend fun deleteFile(rootUri: Uri, relativePath: String): Boolean = withContext(Dispatchers.IO) {
        val docFile = findFile(rootUri, relativePath) ?: return@withContext false
        return@withContext docFile.delete()
    }

    /**
     * Lists directory contents
     */
    suspend fun listDirectory(rootUri: Uri, relativePath: String): List<String> = withContext(Dispatchers.IO) {
        val dirDoc = findFile(rootUri, relativePath) ?: return@withContext emptyList()
        if (!dirDoc.isDirectory) return@withContext emptyList()
        return@withContext dirDoc.listFiles().map { it.name ?: "" }.filter { it.isNotEmpty() && !shouldIgnore(it) }
    }

    /**
     * Grep full text search across the project
     */
    suspend fun searchInProject(rootUri: Uri, query: String): List<String> = withContext(Dispatchers.IO) {
        val results = mutableListOf<String>()
        val rootDoc = DocumentFile.fromTreeUri(context, rootUri) ?: return@withContext emptyList()
        searchRecursive(rootDoc, "", query, results)
        return@withContext results
    }

    private fun searchRecursive(dir: DocumentFile, currentPath: String, query: String, results: MutableList<String>) {
        val files = dir.listFiles()
        for (file in files) {
            val name = file.name ?: continue
            if (shouldIgnore(name)) continue

            val relativePath = if (currentPath.isEmpty()) name else "$currentPath/$name"
            if (file.isDirectory) {
                searchRecursive(file, relativePath, query, results)
            } else {
                try {
                    context.contentResolver.openInputStream(file.uri)?.use { stream ->
                        val reader = BufferedReader(InputStreamReader(stream, Charsets.UTF_8))
                        var lineNum = 1
                        var line: String?
                        while (reader.readLine().also { line = it } != null) {
                            if (line!!.contains(query, ignoreCase = true)) {
                                results.add("$relativePath:$lineNum: ${line!!.trim()}")
                            }
                            lineNum++
                        }
                    }
                } catch (e: Exception) {
                    // Skip files that can't be read as text
                }
            }
        }
    }
}
