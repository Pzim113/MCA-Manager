package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.documentfile.provider.DocumentFile
import androidx.core.content.FileProvider
import com.example.data.database.AppDatabase
import com.example.data.entity.MinecraftFile
import com.example.data.repository.MinecraftFileRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class Screen {
    Home,
    Addons,
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

            // Check if Minecraft package is installed and active
            checkMinecraftInstallation()

            // Remove demonstration files on first run so the workspace starts clean
            repository.deleteDemoFiles()
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
        try {
            val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(uri, takeFlags)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        _storageFolderUri.value = uri.toString()
        viewModelScope.launch {
            repository.saveSetting("storage_folder", uri.toString())
            _statusMessage.value = "Scanning selected directory..."
            
            // Remove demo files when user connects their actual directory so they only see their files!
            repository.deleteDemoFiles()
            
            try {
                val scanned = repository.scanFolder(context, uri)
                _statusMessage.value = if (_currentLanguage.value == AppLanguage.PT) {
                    "Encontrado ${scanned.size} arquivos compatíveis!"
                } else {
                    "Found ${scanned.size} Minecraft compatible files!"
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _statusMessage.value = if (_currentLanguage.value == AppLanguage.PT) "Erro ao ler a pasta!" else "Error reading folder!"
            }
        }
    }

    // --- Clear dynamic scanning and settings ---
    fun clearFolderAndReset() {
        viewModelScope.launch {
            _storageFolderUri.value = null
            repository.removeSetting("storage_folder")
            repository.clearAllFiles()
            showStatus(if (_currentLanguage.value == AppLanguage.PT) "Workspace limpo com sucesso!" else "Workspace cleared successfully!")
        }
    }

    // --- Check Minecraft Bedrock installation and active files/cache ---
    fun checkMinecraftInstallation() {
        viewModelScope.launch {
            val pm = context.packageManager
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
            if (!detected) {
                val checkPaths = listOf(
                    "/storage/emulated/0/games/com.mojang",
                    "/sdcard/games/com.mojang",
                    "/storage/emulated/0/Android/data/com.mojang.minecraftpe/files/games/com.mojang",
                    "/storage/emulated/0/Android/data/com.mojang.minecraftpe/cache"
                )
                detected = checkPaths.any { java.io.File(it).exists() }
            }
            _minecraftInstalled.value = detected
        }
    }

    // --- Dialogs ---
    fun setAppInfoDialogVisible(visible: Boolean) {
        _showAppInfoDialog.value = visible
    }

    fun exportSelectedFiles() {
        val selectedPaths = _selectedFilePaths.value
        if (selectedPaths.isEmpty()) {
            showStatus("No files selected to export!")
            return
        }

        viewModelScope.launch {
            val allFilesNow = files.value
            val targetFiles = allFilesNow.filter { selectedPaths.contains(it.filePath) }
            if (targetFiles.isNotEmpty()) {
                openFilesWithMinecraft(targetFiles)
            }
        }
    }

    fun exportSingleFileToMinecraft(file: MinecraftFile) {
        openFilesWithMinecraft(listOf(file))
    }

    // Direct Import with Minecraft (Action View Trigger - like ZArchiver/Quick Search/Files)
    fun openFilesWithMinecraft(filesToOpen: List<MinecraftFile>) {
        viewModelScope.launch(Dispatchers.IO) {
            checkMinecraftInstallation()
            
            // Create a dedicated cache subdirectory and clear old exported temp files to save space
            val cacheParent = java.io.File(context.cacheDir, "temp_mc_exports")
            try {
                if (cacheParent.exists()) {
                    cacheParent.deleteRecursively()
                }
                cacheParent.mkdirs()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            
            var openedCount = 0
            
            filesToOpen.forEachIndexed { index, file ->
                repository.updateImported(file.filePath, true)
                
                // Keep backup copy in user's Downloads directory
                copyFileToDownloads(file)
                
                try {
                    val fileNameWithExt = "${file.name}.${file.fileType}"
                    val tempFile = java.io.File(cacheParent, fileNameWithExt)
                    
                    var copySuccess = false
                    if (file.isDemo) {
                        try {
                            tempFile.outputStream().use { output ->
                                // Write dummy text to emulate a real MC file
                                output.write("MC_PORTER_DUMMY_DATA_BY_LUA_CREATIVE_FOR_TESTING".toByteArray())
                                copySuccess = true
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    } else {
                        val sourceUri = Uri.parse(file.filePath)
                        try {
                            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                                tempFile.outputStream().use { output ->
                                    input.copyTo(output)
                                    copySuccess = true
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                    
                    if (copySuccess && tempFile.exists()) {
                        val contentUri = FileProvider.getUriForFile(
                            context,
                            "com.example.fileprovider",
                            tempFile
                        )
                        
                        // Action View intent with mimetype and file provider uri
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            setDataAndType(contentUri, "application/octet-stream")
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        
                        // Proactively grant read permission to standard Minecraft packages to be safe
                        val mcPackages = listOf(
                            "com.mojang.minecraftpe",
                            "com.mojang.minecraftpe.playpass",
                            "com.mojang.minecraftedu"
                        )
                        mcPackages.forEach { pkg ->
                            try {
                                context.grantUriPermission(pkg, contentUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            } catch (ex: Exception) {
                                ex.printStackTrace()
                            }
                        }

                        // Proactively query and grant read permission to other matching apps (like ZArchiver etc.)
                        try {
                            val pm = context.packageManager
                            val resolveInfos = pm.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
                            for (resolveInfo in resolveInfos) {
                                val packageName = resolveInfo.activityInfo.packageName
                                try {
                                    context.grantUriPermission(packageName, contentUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                } catch (ex: Exception) {
                                    ex.printStackTrace()
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }

                        // Always present the standard system app chooser with appropriate title, exactly like MCPEDL
                        val chooserTitle = if (_currentLanguage.value == AppLanguage.PT) {
                            "Abrir com o Minecraft"
                        } else {
                            "Open with Minecraft"
                        }
                        
                        val chooser = Intent.createChooser(intent, chooserTitle).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(chooser)
                        
                        openedCount++
                        
                        // Sequential launch delay to allow Minecraft to handle independent import triggers
                        if (filesToOpen.size > 1 && index < filesToOpen.size - 1) {
                            kotlinx.coroutines.delay(1500)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            
            viewModelScope.launch(Dispatchers.Main) {
                val feedback = if (_currentLanguage.value == AppLanguage.PT) {
                    "Selecione o Minecraft na lista para fazer a importação!"
                } else {
                    "Choose Minecraft from the list to import the file!"
                }
                showStatus(feedback)
                clearSelection()
            }
        }
    }

    private fun copyFileToDownloads(file: MinecraftFile): Uri? {
        val fileNameWithExt = "${file.name}.${file.fileType}"
        val resolver = context.contentResolver
        
        // Android 10+ (Q+) using robust MediaStore Downloads API
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            val contentValues = android.content.ContentValues().apply {
                put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, fileNameWithExt)
                put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "application/octet-stream")
                put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS)
            }
            
            val containerUri = android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI
            try {
                // Delete existing entry if any to avoid duplicated clutter on multi-tries
                try {
                    val selection = "${android.provider.MediaStore.MediaColumns.DISPLAY_NAME} = ?"
                    val selectionArgs = arrayOf(fileNameWithExt)
                    resolver.delete(containerUri, selection, selectionArgs)
                } catch (e: Exception) {}
                
                val insertedUri = resolver.insert(containerUri, contentValues)
                if (insertedUri != null) {
                    resolver.openOutputStream(insertedUri)?.use { output ->
                        if (file.isDemo) {
                            output.write("MC_PORTER_DUMMY_DATA_BY_LUA_CREATIVE_FOR_TESTING".toByteArray())
                        } else {
                            val sourceUri = Uri.parse(file.filePath)
                            resolver.openInputStream(sourceUri)?.use { input ->
                                input.copyTo(output)
                            }
                        }
                    }
                    return insertedUri
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        
        // Legacy file direct system fallback
        try {
            val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
            if (!downloadsDir.exists()) {
                downloadsDir.mkdirs()
            }
            val targetFile = java.io.File(downloadsDir, fileNameWithExt)
            if (targetFile.exists()) {
                targetFile.delete()
            }
            targetFile.outputStream().use { output ->
                if (file.isDemo) {
                    output.write("MC_PORTER_DUMMY_DATA_BY_LUA_CREATIVE_FOR_TESTING".toByteArray())
                } else {
                    val sourceUri = Uri.parse(file.filePath)
                    resolver.openInputStream(sourceUri)?.use { input ->
                        input.copyTo(output)
                    }
                }
            }
            return FileProvider.getUriForFile(
                context,
                "com.example.fileprovider",
                targetFile
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    fun downloadFileToSelectedFolder(url: String, userAgent: String?, contentDisposition: String?, mimeType: String?) {
        val folderUriStr = _storageFolderUri.value
        if (folderUriStr == null) {
            val ptMsg = "Por favor, selecione uma pasta de armazenamento nas Configurações primeiro!"
            val enMsg = "Please select a storage folder in Settings first before downloading addons!"
            showStatus(if (_currentLanguage.value == AppLanguage.PT) ptMsg else enMsg)
            return
        }

        val folderUri = Uri.parse(folderUriStr)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val suggestedFileName = android.webkit.URLUtil.guessFileName(url, contentDisposition, mimeType)
                viewModelScope.launch(Dispatchers.Main) {
                    val ptStart = "Iniciando download: $suggestedFileName"
                    val enStart = "Starting download: $suggestedFileName"
                    showStatus(if (_currentLanguage.value == AppLanguage.PT) ptStart else enStart)
                }

                val connection = java.net.URL(url).openConnection() as java.net.HttpURLConnection
                connection.connectTimeout = 15000
                connection.readTimeout = 15000
                userAgent?.let { connection.setRequestProperty("User-Agent", it) }
                
                // Add cookies to download authenticate session if present
                val cookies = android.webkit.CookieManager.getInstance().getCookie(url)
                if (!cookies.isNullOrEmpty()) {
                    connection.setRequestProperty("Cookie", cookies)
                }
                
                connection.connect()

                if (connection.responseCode in 200..299) {
                    val parentDir = DocumentFile.fromTreeUri(context, folderUri)
                    if (parentDir != null && parentDir.exists()) {
                        val docFile = parentDir.createFile(mimeType ?: "application/octet-stream", suggestedFileName)
                        if (docFile != null) {
                            context.contentResolver.openOutputStream(docFile.uri)?.use { outputStream ->
                                connection.inputStream.use { inputStream ->
                                    inputStream.copyTo(outputStream)
                                }
                            }

                            // Read details to register in DB so it shows up on the screen immediately
                            val finalFileName = docFile.name ?: suggestedFileName
                            val extension = finalFileName.substringAfterLast('.', "").lowercase()
                            val size = docFile.length()
                            val newMcFile = MinecraftFile(
                                filePath = docFile.uri.toString(),
                                name = finalFileName.substringBeforeLast("."),
                                fileSize = size,
                                fileType = extension,
                                isDemo = false
                            )
                            repository.insertFile(newMcFile)

                            viewModelScope.launch(Dispatchers.Main) {
                                val ptSuccess = "Download concluído! Salvo com sucesso."
                                val enSuccess = "Download complete! Saved to workspace folder."
                                showStatus(if (_currentLanguage.value == AppLanguage.PT) ptSuccess else enSuccess)
                            }
                        } else {
                            viewModelScope.launch(Dispatchers.Main) {
                                val ptErr = "Falha ao gravar na pasta selecionada!"
                                val enErr = "Failed to write file to selected workspace folder!"
                                showStatus(if (_currentLanguage.value == AppLanguage.PT) ptErr else enErr)
                            }
                        }
                    } else {
                        viewModelScope.launch(Dispatchers.Main) {
                            val ptFolderErr = "Pasta de armazenamento inválida nas configurações!"
                            val enFolderErr = "Invalid workspace storage folder selected!"
                            showStatus(if (_currentLanguage.value == AppLanguage.PT) ptFolderErr else enFolderErr)
                        }
                    }
                } else {
                    viewModelScope.launch(Dispatchers.Main) {
                        val ptFail = "Erro no download: Código do servidor ${connection.responseCode}"
                        val enFail = "Download failed: Server returned ${connection.responseCode}"
                        showStatus(if (_currentLanguage.value == AppLanguage.PT) ptFail else enFail)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                viewModelScope.launch(Dispatchers.Main) {
                    val ptException = "Erro durante o download: ${e.localizedMessage}"
                    val enException = "Download error: ${e.localizedMessage}"
                    showStatus(if (_currentLanguage.value == AppLanguage.PT) ptException else enException)
                }
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
