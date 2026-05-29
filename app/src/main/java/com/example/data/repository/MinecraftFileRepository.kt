package com.example.data.repository

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.documentfile.provider.DocumentFile
import com.example.data.dao.AppSettingDao
import com.example.data.dao.MinecraftFileDao
import com.example.data.entity.AppSetting
import com.example.data.entity.MinecraftFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class MinecraftFileRepository(
    private val minecraftFileDao: MinecraftFileDao,
    private val appSettingDao: AppSettingDao
) {

    val allFiles: Flow<List<MinecraftFile>> = minecraftFileDao.getAllFiles()

    fun getFilesByType(type: String): Flow<List<MinecraftFile>> {
        return if (type.lowercase() == "all" || type.isEmpty()) {
            minecraftFileDao.getAllFiles()
        } else {
            minecraftFileDao.getFilesByType(type.lowercase())
        }
    }

    suspend fun insertFile(file: MinecraftFile) = withContext(Dispatchers.IO) {
        minecraftFileDao.insertFile(file)
    }

    suspend fun insertFiles(files: List<MinecraftFile>) = withContext(Dispatchers.IO) {
        minecraftFileDao.insertFiles(files)
    }

    suspend fun updateFavorite(path: String, isFavorite: Boolean) = withContext(Dispatchers.IO) {
        minecraftFileDao.updateFavorite(path, isFavorite)
    }

    suspend fun updateImported(path: String, isImported: Boolean) = withContext(Dispatchers.IO) {
        minecraftFileDao.updateImported(path, isImported)
    }

    suspend fun deleteFile(file: MinecraftFile) = withContext(Dispatchers.IO) {
        minecraftFileDao.deleteFile(file)
    }

    suspend fun deleteFileByPath(path: String) = withContext(Dispatchers.IO) {
        minecraftFileDao.deleteFileByPath(path)
    }

    suspend fun deleteDemoFiles() = withContext(Dispatchers.IO) {
        minecraftFileDao.deleteDemoFiles()
    }

    suspend fun clearAllFiles() = withContext(Dispatchers.IO) {
        minecraftFileDao.clearAllFiles()
    }

    // --- Settings Persistence ---
    suspend fun saveSetting(key: String, value: String) = withContext(Dispatchers.IO) {
        appSettingDao.saveSetting(AppSetting(key, value))
    }

    suspend fun getSetting(key: String): String? = withContext(Dispatchers.IO) {
        appSettingDao.getSettingByKey(key)?.value
    }

    fun observeSetting(key: String): Flow<AppSetting?> {
        return appSettingDao.observeSettingByKey(key)
    }

    suspend fun removeSetting(key: String) = withContext(Dispatchers.IO) {
        appSettingDao.removeSetting(key)
    }

    // --- Prepopulate Demo files ---
    suspend fun prepopulateDemoFiles() = withContext(Dispatchers.IO) {
        val demoList = listOf(
            MinecraftFile(
                filePath = "demo://assets/modern_mansion_v2.mcworld",
                name = "Modern Cliffside Mansion Pack",
                fileSize = 25680192, // ~24.5 MB
                fileType = "mcworld",
                isDemo = true,
                isFavorite = true
            ),
            MinecraftFile(
                filePath = "demo://assets/cyberpunk_resource_pack.mcpack",
                name = "Cyberpunk Neon Retexture Pack",
                fileSize = 12687769, // ~12.1 MB
                fileType = "mcpack",
                isDemo = true,
                isFavorite = false
            ),
            MinecraftFile(
                filePath = "demo://assets/advanced_furniture.mcaddon",
                name = "Ultra Furniture & Tech Addon",
                fileSize = 8808038, // ~8.4 MB
                fileType = "mcaddon",
                isDemo = true,
                isFavorite = true
            ),
            MinecraftFile(
                filePath = "demo://assets/obsidian_gate.mcstructure",
                name = "Nether Portal Automator Blueprint",
                fileSize = 126976, // 124 KB
                fileType = "mcstructure",
                isDemo = true,
                isFavorite = false
            ),
            MinecraftFile(
                filePath = "demo://assets/ocean_monument_map.mctemplate",
                name = "Guardian Farm Survival Template",
                fileSize = 4515840, // 4.3 MB
                fileType = "mctemplate",
                isDemo = true,
                isFavorite = false
            ),
            MinecraftFile(
                filePath = "demo://assets/bedrock_tweaks.mcmeta",
                name = "FPS Performance Optimizer Config",
                fileSize = 12288, // 12 KB
                fileType = "mcmeta",
                isDemo = true,
                isFavorite = false
            ),
            MinecraftFile(
                filePath = "demo://assets/survival_realm_backup.mcproject",
                name = "Multiplayer Realm World Project",
                fileSize = 1887436, // 1.8 MB
                fileType = "mcproject",
                isDemo = true,
                isFavorite = false
            )
        )
        minecraftFileDao.insertFiles(demoList)
    }

    // --- Dynamic file scanning ---
    // Scans a folder selected via Storage Access Framework
    suspend fun scanFolder(context: Context, folderUri: Uri): List<MinecraftFile> = withContext(Dispatchers.IO) {
        val filesFound = mutableListOf<MinecraftFile>()
        val parentDir = DocumentFile.fromTreeUri(context, folderUri)
        
        if (parentDir != null && parentDir.isDirectory) {
            val childFiles = parentDir.listFiles()
            childFiles.forEach { file ->
                if (file.isFile) {
                    val fileName = file.name ?: return@forEach
                    val extension = getFileExtension(fileName)
                    
                    if (isSupportedMinecraftExtension(extension)) {
                        val size = file.length()
                        val filePath = file.uri.toString()
                        
                        filesFound.add(
                            MinecraftFile(
                                filePath = filePath,
                                name = fileName.substringBeforeLast("."),
                                fileSize = size,
                                fileType = extension.lowercase(),
                                isDemo = false
                            )
                        )
                    }
                }
            }
        }
        
        if (filesFound.isNotEmpty()) {
            minecraftFileDao.insertFiles(filesFound)
        }
        
        filesFound
    }

    private fun getFileExtension(fileName: String): String {
        val lastIdx = fileName.lastIndexOf('.')
        return if (lastIdx != -1 && lastIdx < fileName.length - 1) {
            fileName.substring(lastIdx + 1).lowercase()
        } else {
            ""
        }
    }

    private fun isSupportedMinecraftExtension(ext: String): Boolean {
        val supported = setOf("mcaddon", "mcpack", "mcworld", "mctemplate", "mcstructure", "mcmeta", "mcproject")
        return supported.contains(ext.lowercase())
    }
}
