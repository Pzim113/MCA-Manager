package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.entity.MinecraftFile
import com.example.data.repository.MinecraftFileRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class Screen {
    Home,
    Settings
}

enum class AppLanguage(val code: String, val displayName: String) {
    EN("en", "English"),
    PT("pt", "Português")
}

class McaViewModel(application: Application) : AndroidViewModel(application) {

    private val context: Context get() = getApplication()
    private val database = AppDatabase.getDatabase(application)
    private val repository = MinecraftFileRepository(
        database.minecraftFileDao(),
        database.appSettingDao()
    )

    // --- State Variables ---
    private val _currentScreen = MutableStateFlow(Screen.Home)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    private val _currentLanguage = MutableStateFlow(AppLanguage.EN)
    val currentLanguage: StateFlow<AppLanguage> = _currentLanguage.asStateFlow()

    private val _selectedFilter = MutableStateFlow("all")
    val selectedFilter: StateFlow<String> = _selectedFilter.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedFilePaths = MutableStateFlow<Set<String>>(emptySet())
    val selectedFilePaths: StateFlow<Set<String>> = _selectedFilePaths.asStateFlow()

    private val _storageFolderUri = MutableStateFlow<String?>(null)
    val storageFolderUri: StateFlow<String?> = _storageFolderUri.asStateFlow()

    private val _minecraftInstalled = MutableStateFlow(false)
    val minecraftInstalled: StateFlow<Boolean> = _minecraftInstalled.asStateFlow()

    private val _showAppInfoDialog = MutableStateFlow(false)
    val showAppInfoDialog: StateFlow<Boolean> = _showAppInfoDialog.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    // --- List of loaded Minecraft Files from Database ---
    val files: StateFlow<List<MinecraftFile>> = combine(
        _selectedFilter,
        _searchQuery
    ) { filter, query ->
        Pair(filter, query)
    }.flatMapLatest { (filter, query) ->
        repository.getFilesByType(filter).map { fileList ->
            var result = fileList
            if (query.isNotEmpty()) {
                result = result.filter { file ->
                    file.name.contains(query, ignoreCase = true) ||
                    file.fileType.contains(query, ignoreCase = true)
                }
            }
            result
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    init {
        viewModelScope.launch {
            // Observe or Load saved settings from database
            val savedLanguage = repository.getSetting("language") ?: "en"
            _currentLanguage.value = if (savedLanguage == "pt") AppLanguage.PT else AppLanguage.EN
            
            val savedFolder = repository.getSetting("storage_folder")
            _storageFolderUri.value = savedFolder

            // Check if Minecraft package is installed
            checkMinecraftInstallation()

            // Pre-populate Database with beautiful curated demo items on very first run
            repository.allFiles.first().let { currentList ->
                if (currentList.isEmpty()) {
                    repository.prepopulateDemoFiles()
                }
            }
        }
    }

    // --- Screen Control ---
    fun setScreen(screen: Screen) {
        _currentScreen.value = screen
    }

    // --- Language Change ---
    fun setLanguage(language: AppLanguage) {
        _currentLanguage.value = language
        viewModelScope.launch {
            repository.saveSetting("language", language.code)
        }
    }

    // --- Filter Control ---
    fun setFilter(filter: String) {
        _selectedFilter.value = filter
        // Clear selection when changing filters
        clearSelection()
    }

    // --- Search Query Change ---
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    // --- Multi-Selection Actions ---
    fun toggleFileSelection(filePath: String) {
        val currentSet = _selectedFilePaths.value.toMutableSet()
        if (currentSet.contains(filePath)) {
            currentSet.remove(filePath)
        } else {
            currentSet.add(filePath)
        }
        _selectedFilePaths.value = currentSet
    }

    fun selectAllFiles(allPaths: List<String>) {
        _selectedFilePaths.value = allPaths.toSet()
    }

    fun clearSelection() {
        _selectedFilePaths.value = emptySet()
    }

    // --- Delete File ---
    fun deleteMinecraftFile(file: MinecraftFile) {
        viewModelScope.launch {
            repository.deleteFile(file)
            showStatus("Deleted: ${file.name}.${file.fileType}")
        }
    }

    // --- Toggle Favorite ---
    fun toggleFavorite(file: MinecraftFile) {
        viewModelScope.launch {
            repository.updateFavorite(file.filePath, !file.isFavorite)
        }
    }

    // --- Folder Scanning Callback ---
    fun updateStorageFolder(uri: Uri) {
        _storageFolderUri.value = uri.toString()
        viewModelScope.launch {
            repository.saveSetting("storage_folder", uri.toString())
            _statusMessage.value = "Scanning selected directory..."
            
            // Remove demo files when user connects their actual directory so they only see their files!
            repository.deleteDemoFiles()
            
            val scanned = repository.scanFolder(context, uri)
            _statusMessage.value = "Found ${scanned.size} Minecraft compatible files!"
        }
    }

    // --- Clear dynamic scanning and restore demo files ---
    fun clearFolderAndResetDemo() {
        viewModelScope.launch {
            _storageFolderUri.value = null
            repository.removeSetting("storage_folder")
            repository.clearAllFiles()
            repository.prepopulateDemoFiles()
            showStatus("Database reset to showcase demo files.")
        }
    }

    // --- Check Minecraft Bedrock installation ---
    fun checkMinecraftInstallation() {
        viewModelScope.launch {
            val pm = context.packageManager
            // Common Minecraft package IDs
            val mcPackages = listOf(
                "com.mojang.minecraftpe",
                "com.mojang.minecraftpe.playpass"
            )
            var detected = false
            for (pkg in mcPackages) {
                try {
                    pm.getPackageInfo(pkg, PackageManager.GET_ACTIVITIES)
                    detected = true
                    break
                } catch (e: PackageManager.NameNotFoundException) {
                    // Not found, check next
                }
            }
            _minecraftInstalled.value = detected
        }
    }

    // --- Dialogs ---
    fun setAppInfoDialogVisible(visible: Boolean) {
        _showAppInfoDialog.value = visible
    }

    // --- Export files dynamically to Minecraft Bedrock ---
    fun exportSelectedFiles() {
        val selectedPaths = _selectedFilePaths.value
        if (selectedPaths.isEmpty()) {
            showStatus("No files selected to export!")
            return
        }

        viewModelScope.launch {
            // Find files details
            val allFilesNow = files.value
            val targetFiles = allFilesNow.filter { selectedPaths.contains(it.filePath) }
            
            if (targetFiles.size == 1) {
                exportSingleFileToMinecraft(targetFiles.first())
            } else if (targetFiles.isNotEmpty()) {
                // Bulk export / share to avoid spawning multiple parallel chooser activities
                targetFiles.forEach { repository.updateImported(it.filePath, true) }
                checkMinecraftInstallation()

                val intent = Intent().apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
                }

                val hasRealFiles = targetFiles.any { !it.isDemo }
                if (hasRealFiles) {
                    intent.action = Intent.ACTION_SEND_MULTIPLE
                    intent.type = "application/octet-stream"
                    val uris = ArrayList<Uri>()
                    targetFiles.forEach {
                        if (!it.isDemo) {
                            try {
                                uris.add(Uri.parse(it.filePath))
                            } catch (e: Exception) {}
                        }
                    }
                    intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                } else {
                    intent.action = Intent.ACTION_SEND
                    intent.type = "text/plain"
                    val textBuilder = StringBuilder("Importing multiple packs to Minecraft:\n")
                    targetFiles.forEach {
                        textBuilder.append("- ${it.name}.${it.fileType} (${it.fileSize} bytes)\n")
                    }
                    intent.putExtra(Intent.EXTRA_SUBJECT, "MCA Manager Bulk Export")
                    intent.putExtra(Intent.EXTRA_TEXT, textBuilder.toString())
                }

                try {
                    val chooser = Intent.createChooser(intent, "Export packs to Minecraft")
                    chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(chooser)
                } catch (e: Exception) {
                    Toast.makeText(context, "Direct import unavailable on empty simulator.", Toast.LENGTH_SHORT).show()
                }
            }
            
            clearSelection()
            showStatus(if (_currentLanguage.value == AppLanguage.PT) "Exportação concluída!" else "Export completed successfully!")
        }
    }

    fun exportSingleFileToMinecraft(file: MinecraftFile) {
        viewModelScope.launch {
            // Mark as imported in db
            repository.updateImported(file.filePath, true)
            
            // Re-check installation to support dynamic detections
            checkMinecraftInstallation()

            // Actual Android Open Intent implementation using sharing/view action
            // Standard action for Minecraft file extensions is INTENT ACTION_VIEW with application/octet-stream or custom mime-type
            try {
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/octet-stream"
                    putExtra(Intent.EXTRA_TITLE, "${file.name}.${file.fileType}")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
                }
                
                if (file.isDemo) {
                    // For demo placeholders, we share a friendly text stream or link to Minecraft schemas
                    intent.setAction(Intent.ACTION_SEND)
                    intent.type = "text/plain"
                    intent.putExtra(Intent.EXTRA_SUBJECT, "MCA Manager Export")
                    intent.putExtra(Intent.EXTRA_TEXT, "Importing ${file.name}.${file.fileType} to Minecraft. Size: ${file.fileSize} bytes.")
                } else {
                    // For actual local files, use the real URI
                    try {
                        val fileUri = Uri.parse(file.filePath)
                        intent.putExtra(Intent.EXTRA_STREAM, fileUri)
                        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    } catch (e: Exception) {
                        // Fallback Text if Uri parsing failed
                        intent.setAction(Intent.ACTION_SEND)
                        intent.type = "text/plain"
                        intent.putExtra(Intent.EXTRA_TEXT, "File URI: ${file.filePath}")
                    }
                }

                // Create chooser
                val chooser = Intent.createChooser(intent, "Open with Minecraft Bedrock")
                chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(chooser)
                
            } catch (e: Exception) {
                // If sharing failed, fallback to Toast
                Toast.makeText(context, "Direct import unavailable on empty simulator. Launching system chooser.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun showStatus(msg: String) {
        _statusMessage.value = msg
        // Simple auto-dismiss trigger
        viewModelScope.launch {
            kotlinx.coroutines.delay(3000)
            if (_statusMessage.value == msg) {
                _statusMessage.value = null
            }
        }
    }
}
