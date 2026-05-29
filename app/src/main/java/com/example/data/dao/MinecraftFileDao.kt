package com.example.data.dao

import androidx.room.*
import com.example.data.entity.MinecraftFile
import kotlinx.coroutines.flow.Flow

@Dao
interface MinecraftFileDao {

    @Query("SELECT * FROM minecraft_files ORDER BY isFavorite DESC, importDate DESC")
    fun getAllFiles(): Flow<List<MinecraftFile>>

    @Query("SELECT * FROM minecraft_files WHERE fileType = :type ORDER BY isFavorite DESC, importDate DESC")
    fun getFilesByType(type: String): Flow<List<MinecraftFile>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFile(file: MinecraftFile)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFiles(files: List<MinecraftFile>)

    @Query("UPDATE minecraft_files SET isFavorite = :favorite WHERE filePath = :path")
    suspend fun updateFavorite(path: String, favorite: Boolean)

    @Query("UPDATE minecraft_files SET isImported = :imported WHERE filePath = :path")
    suspend fun updateImported(path: String, imported: Boolean)

    @Delete
    suspend fun deleteFile(file: MinecraftFile)

    @Query("DELETE FROM minecraft_files WHERE filePath = :path")
    suspend fun deleteFileByPath(path: String)

    @Query("DELETE FROM minecraft_files WHERE isDemo = 1")
    suspend fun deleteDemoFiles()

    @Query("DELETE FROM minecraft_files")
    suspend fun clearAllFiles()
}
