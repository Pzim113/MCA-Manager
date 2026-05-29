package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "minecraft_files")
data class MinecraftFile(
    @PrimaryKey val filePath: String, // Full file path or URI acting as unique key
    val name: String,
    val fileSize: Long,
    val fileType: String, // e.g., "mcaddon", "mcpack", "mcworld", "mctemplate", "mcstructure", "mcmeta", "mcproject"
    val importDate: Long = System.currentTimeMillis(),
    val isImported: Boolean = false,
    val isFavorite: Boolean = false,
    val isDemo: Boolean = false
)
