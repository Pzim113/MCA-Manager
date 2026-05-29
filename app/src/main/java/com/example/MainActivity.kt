package com.example

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.activity.compose.BackHandler
import androidx.compose.ui.viewinterop.AndroidView
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebResourceRequest
import com.example.data.entity.MinecraftFile
import com.example.ui.theme.*
import com.example.ui.viewmodel.AppLanguage
import com.example.ui.viewmodel.McaViewModel
import com.example.ui.viewmodel.Screen

// --- Inline Localization Mappings ---
class Localization(val language: AppLanguage) {
    val appTitle: String get() = "MCA Manager"
    val filesTab: String get() = if (language == AppLanguage.PT) "Arquivos" else "Files"
    val addonsTab: String get() = if (language == AppLanguage.PT) "Baixar" else "Get Addons"
    val addonsTitle: String get() = if (language == AppLanguage.PT) "Obter Addons" else "Get Addons"
    val addonsDesc: String get() = if (language == AppLanguage.PT) "Baixe complementos de fontes seguras direto para sua pasta de arquivos" else "Download packages from community sources straight to your workspace directory"
    val settingsTab: String get() = if (language == AppLanguage.PT) "Ajustes" else "Settings"
    val searchPlaceholder: String get() = if (language == AppLanguage.PT) "Buscar arquivos..." else "Search files..."
    val noFilesFound: String get() = if (language == AppLanguage.PT) "Nenhum arquivo Minecraft detectado" else "No matching Minecraft files"
    val scanButton: String get() = if (language == AppLanguage.PT) "Escanear Pasta" else "Scan Storage Folder"
    val selectFolderTip: String get() = if (language == AppLanguage.PT) 
        "Toque no botão abaixo para selecionar a pasta de download ou games e ler arquivos .mcaddon, .mcpack, etc." 
        else "Tap the button below to pick your downloads or games directory and index custom packs immediately."
    val exportSelected: String get() = if (language == AppLanguage.PT) "Abrir com o Minecraft" else "Open with Minecraft"
    val fileTypeAll: String get() = if (language == AppLanguage.PT) "Todos" else "All"
    val itemsSelectedOne: String get() = if (language == AppLanguage.PT) "selecionado" else "selected"
    val itemsSelectedMore: String get() = if (language == AppLanguage.PT) "selecionados" else "selected"
    val selectAll: String get() = if (language == AppLanguage.PT) "Selecionar Tudo" else "Select All"
    val deselectAll: String get() = if (language == AppLanguage.PT) "Limpar Seleção" else "Deselect All"
    val fileInfoLabel: String get() = if (language == AppLanguage.PT) "Aberto" else "Opened"
    val demoLabel: String get() = if (language == AppLanguage.PT) "SAMP" else "DEMO"
    
    // Settings translations
    val settingsTitle: String get() = if (language == AppLanguage.PT) "Configurações" else "Application Settings"
    val settingsLanguageTitle: String get() = if (language == AppLanguage.PT) "Idioma do Aplicativo" else "App Language"
    val settingsLanguageDesc: String get() = if (language == AppLanguage.PT) "Alternar entre Inglês e Português" else "Switch between English and Portuguese"
    val settingsStorageTitle: String get() = if (language == AppLanguage.PT) "Diretório de Armazenamento" else "Storage Directory"
    val settingsStorageDesc: String get() = if (language == AppLanguage.PT) "Pasta selecionada para leitura rápida" else "Directory selected for scanning content"
    val settingsStorageNotSet: String get() = if (language == AppLanguage.PT) "Nenhum diretório conectado" else "No directory connected"
    val settingsMinecraftTitle: String get() = if (language == AppLanguage.PT) "Minecraft Bedrock" else "Minecraft Bedrock Status"
    val settingsMinecraftDoc: String get() = if (language == AppLanguage.PT) "Verificar se o jogo oficial está instalado" else "Scan if the official game is installed"
    val settingsMinecraftDetected: String get() = if (language == AppLanguage.PT) "Minecraft Detectado" else "Minecraft Detected"
    val settingsMinecraftNotDetected: String get() = if (language == AppLanguage.PT) "Minecraft Não Detectado (Toque p/ verificar)" else "Minecraft Not Detected (Tap to verify)"
    val settingsAppInfoTitle: String get() = if (language == AppLanguage.PT) "Informações do Sistema" else "System Information"
    val settingsAppInfoDesc: String get() = if (language == AppLanguage.PT) "Sobre, versão, termos e criadores" else "About, version, licenses, and creators"
    val resetStorageButton: String get() = if (language == AppLanguage.PT) "Limpar Pasta" else "Clear Folder"
    
    // About Dialog
    val aboutTitle: String get() = if (language == AppLanguage.PT) "Sobre o MCA Manager" else "About MCA Manager"
    val aboutVersion: String get() = if (language == AppLanguage.PT) "Versão 1.0.0 (Ajustes Rápidos)" else "Version 1.0.0 (High-Speed Import)"
    val aboutDescription: String get() = if (language == AppLanguage.PT) 
        "O MCA Manager foi desenvolvido para gerenciar e importar em alta velocidade complementos, mundos e pacotes de texturas customizados (.mcaddon, .mcpack, .mcworld, .mctemplate, .mcstructure, .mcmeta, .mcproject) direto no Minecraft Bedrock Edition."
        else "MCA Manager is a utility built to index and install custom addons, maps, behavior packs, textures, blueprints, and project models (.mcaddon, .mcpack, .mcworld, .mctemplate, .mcstructure, .mcmeta, .mcproject) directly into Minecraft Bedrock Edition."
    val aboutCredits: String get() = if (language == AppLanguage.PT) "Livre de anúncios • Focado em desempenho e simplicidade." else "Ad-free • Designed with material precision and velocity."
    val closeButton: String get() = if (language == AppLanguage.PT) "Fechar" else "Close"
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainAppScreen()
            }
        }
    }
}

@Composable
fun MainAppScreen() {
    val viewModel: McaViewModel = viewModel()
    val context = LocalContext.current

    // Observe State flows
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val currentLanguage by viewModel.currentLanguage.collectAsStateWithLifecycle()
    val selectedFilter by viewModel.selectedFilter.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedFilePaths by viewModel.selectedFilePaths.collectAsStateWithLifecycle()
    val storageFolderUri by viewModel.storageFolderUri.collectAsStateWithLifecycle()
    val minecraftInstalled by viewModel.minecraftInstalled.collectAsStateWithLifecycle()
    val showAppInfoDialog by viewModel.showAppInfoDialog.collectAsStateWithLifecycle()
    val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()
    val filesList by viewModel.files.collectAsStateWithLifecycle()

    val local = remember(currentLanguage) { Localization(currentLanguage) }

    // SAF Directory picker configuration
    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.updateStorageFolder(it)
        }
    }

    // Trigger toast messages when status changes
    LaunchedEffect(statusMessage) {
        statusMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            // Sleek bottom navigation bar following the theme direction
            NavigationBar(
                containerColor = ObsidianSurface,
                tonalElevation = 8.dp,
                modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                NavigationBarItem(
                    selected = currentScreen == Screen.Home,
                    onClick = { viewModel.setScreen(Screen.Home) },
                    icon = { Icon(Icons.Filled.Folder, contentDescription = local.filesTab) },
                    label = { Text(local.filesTab) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = ObsidianBg,
                        selectedTextColor = MinecraftGreen,
                        indicatorColor = MinecraftGreen,
                        unselectedIconColor = TextMuted,
                        unselectedTextColor = TextMuted
                    ),
                    modifier = Modifier.testTag("nav_home_tab")
                )
                NavigationBarItem(
                    selected = currentScreen == Screen.Addons,
                    onClick = { viewModel.setScreen(Screen.Addons) },
                    icon = { Icon(Icons.Filled.Download, contentDescription = local.addonsTab) },
                    label = { Text(local.addonsTab) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = ObsidianBg,
                        selectedTextColor = MinecraftGreen,
                        indicatorColor = MinecraftGreen,
                        unselectedIconColor = TextMuted,
                        unselectedTextColor = TextMuted
                    ),
                    modifier = Modifier.testTag("nav_addons_tab")
                )
                NavigationBarItem(
                    selected = currentScreen == Screen.Settings,
                    onClick = { viewModel.setScreen(Screen.Settings) },
                    icon = { Icon(Icons.Filled.Settings, contentDescription = local.settingsTab) },
                    label = { Text(local.settingsTab) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = ObsidianBg,
                        selectedTextColor = MinecraftGreen,
                        indicatorColor = MinecraftGreen,
                        unselectedIconColor = TextMuted,
                        unselectedTextColor = TextMuted
                    ),
                    modifier = Modifier.testTag("nav_settings_tab")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(ObsidianBg)
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = currentScreen,
                transitionSpec = {
                    fadeIn(animationSpec = spring()) togetherWith fadeOut(animationSpec = spring())
                },
                label = "ScreenTransition"
            ) { screen ->
                when (screen) {
                    Screen.Home -> HomeScreen(
                        viewModel = viewModel,
                        filesList = filesList,
                        selectedFilter = selectedFilter,
                        searchQuery = searchQuery,
                        selectedFilePaths = selectedFilePaths,
                        local = local,
                        onTriggerPicker = { folderPickerLauncher.launch(null) }
                    )
                    Screen.Addons -> AddonsScreen(
                        viewModel = viewModel,
                        local = local
                    )
                    Screen.Settings -> SettingsScreen(
                        viewModel = viewModel,
                        storageFolderUri = storageFolderUri,
                        minecraftInstalled = minecraftInstalled,
                        local = local,
                        onTriggerPicker = { folderPickerLauncher.launch(null) }
                    )
                }
            }

            // High-precision App Info Dialog
            if (showAppInfoDialog) {
                AppInfoDialog(
                    local = local,
                    onDismiss = { viewModel.setAppInfoDialogVisible(false) }
                )
            }
        }
    }
}

@Composable
fun HomeScreen(
    viewModel: McaViewModel,
    filesList: List<MinecraftFile>,
    selectedFilter: String,
    searchQuery: String,
    selectedFilePaths: Set<String>,
    local: Localization,
    onTriggerPicker: () -> Unit
) {
    val context = LocalContext.current
    var isSearchExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
    ) {
        // App header with beautiful Immersive UI details
        Spacer(modifier = Modifier.height(28.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (!isSearchExpanded) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text(
                            text = local.appTitle,
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = (-0.5).sp,
                            fontFamily = FontFamily.SansSerif,
                            modifier = Modifier.testTag("app_title")
                        )
                        Text(
                            text = if (local.language == AppLanguage.PT) "ESPAÇO DE TRABALHO" else "FILE WORKSPACE",
                            color = MinecraftGreen.copy(alpha = 0.8f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Styled circular search toggle button
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(ObsidianSurface)
                            .border(1.dp, Color.White.copy(alpha = 0.05f), CircleShape)
                            .clickable { isSearchExpanded = true }
                            .testTag("search_toggle"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Search, contentDescription = "Search", tint = MinecraftGreen, modifier = Modifier.size(20.dp))
                    }
                }
            } else {
                // Expanded search field
                TextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    placeholder = { Text(local.searchPlaceholder, color = TextMuted) },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = MinecraftGreen) },
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                viewModel.setSearchQuery("")
                                isSearchExpanded = false
                            }
                        ) {
                            Icon(Icons.Filled.Close, contentDescription = "Close Search", tint = TextPrimary)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, MinecraftGreen.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                        .testTag("search_input"),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = ObsidianSurface,
                        unfocusedContainerColor = ObsidianSurface,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Multi-select status banner & filters
        if (selectedFilePaths.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MinecraftGreen.copy(alpha = 0.12f))
                    .border(1.dp, MinecraftGreen.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${selectedFilePaths.size} ${if (selectedFilePaths.size == 1) local.itemsSelectedOne else local.itemsSelectedMore}",
                    color = MinecraftGreen,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Row {
                    TextButton(
                        onClick = {
                            val selectablePaths = filesList.map { it.filePath }
                            if (selectedFilePaths.size == filesList.size) {
                                viewModel.clearSelection()
                            } else {
                                viewModel.selectAllFiles(selectablePaths)
                            }
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = TextPrimary)
                    ) {
                        Text(
                            text = if (selectedFilePaths.size == filesList.size) local.deselectAll else local.selectAll,
                            fontSize = 12.sp
                        )
                    }
                    IconButton(
                        onClick = { viewModel.clearSelection() }
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = "Cancel", tint = TextPrimary, modifier = Modifier.size(18.dp))
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Horizontal filter row
        val filterTypes = listOf(
            Pair("all", local.fileTypeAll),
            Pair("mcaddon", ".mcaddon"),
            Pair("mcpack", ".mcpack"),
            Pair("mcworld", ".mcworld"),
            Pair("mctemplate", ".mctemplate"),
            Pair("mcstructure", ".mcstructure"),
            Pair("mcmeta", ".mcmeta"),
            Pair("mcproject", ".mcproject")
        )

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 8.dp)
        ) {
            items(filterTypes) { (typeCode, displayName) ->
                val isSelected = selectedFilter == typeCode
                val uppercaseName = displayName.uppercase()
                FilterChip(
                    selected = isSelected,
                    onClick = { viewModel.setFilter(typeCode) },
                    label = { 
                        Text(
                            text = uppercaseName, 
                            fontSize = 11.sp, 
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            letterSpacing = 0.5.sp,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        ) 
                    },
                    shape = CircleShape,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MinecraftGreenVariant,
                        selectedLabelColor = MinecraftGreen,
                        containerColor = ObsidianSurface,
                        labelColor = TextSecondary
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isSelected,
                        selectedBorderColor = MinecraftGreen.copy(alpha = 0.2f),
                        borderColor = Color.White.copy(alpha = 0.05f)
                    ),
                    modifier = Modifier.testTag("file_filter_chip_$typeCode")
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Minecraft Files List
        if (filesList.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    // Minecraft themed chest / folder vector icon
                    Icon(
                        imageVector = Icons.Filled.Inbox,
                        contentDescription = "Empty",
                        tint = TextMuted,
                        modifier = Modifier.size(72.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = local.noFilesFound,
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = local.selectFolderTip,
                        color = TextMuted,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = onTriggerPicker,
                        colors = ButtonDefaults.buttonColors(containerColor = ObsidianSurfaceVariant),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Filled.FolderOpen, contentDescription = null, tint = MinecraftGreen)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(local.scanButton, color = TextPrimary)
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .testTag("file_list"),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 100.dp, top = 4.dp)
            ) {
                items(filesList) { item ->
                    val isChecked = selectedFilePaths.contains(item.filePath)
                    FileListItem(
                        file = item,
                        selected = isChecked,
                        local = local,
                        onCheckedChange = { viewModel.toggleFileSelection(item.filePath) },
                        onFavoriteToggle = { viewModel.toggleFavorite(item) },
                        onExportClick = { viewModel.exportSingleFileToMinecraft(item) },
                        onDeleteClick = { viewModel.deleteMinecraftFile(item) }
                    )
                }
            }
        }
    }

    // Floating action button at the bottom - only visible when files are selected
    val hasSelection = selectedFilePaths.isNotEmpty()
    if (hasSelection) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Button(
                onClick = {
                    viewModel.exportSelectedFiles()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MinecraftGreen,
                    contentColor = ObsidianBg
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 10.dp),
                contentPadding = PaddingValues(horizontal = 24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .clip(RoundedCornerShape(32.dp))
                    .border(
                        width = 1.dp,
                        color = MinecraftGreen.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(32.dp)
                    )
                    .testTag("large_action_button")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.DriveFileMove,
                        contentDescription = null,
                        tint = ObsidianBg,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "${local.exportSelected} (${selectedFilePaths.size})".uppercase(),
                        color = ObsidianBg,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    )
                }
            }
        }
    }
}

@Composable
fun FileListItem(
    file: MinecraftFile,
    selected: Boolean,
    local: Localization,
    onCheckedChange: (Boolean) -> Unit,
    onFavoriteToggle: () -> Unit,
    onExportClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val context = LocalContext.current
    val colorIdentity = when (file.fileType.lowercase()) {
        "mcaddon" -> MinecraftGreen
        "mcpack" -> Color(0xFFE040FB) // Beautiful Purple
        "mcworld" -> Color(0xFF2979FF)  // Deep Blue
        "mctemplate" -> Color(0xFFFF9100) // Deep Orange
        "mcstructure" -> DiamondCyan
        "mcmeta" -> GoldYellow
        "mcproject" -> Color(0xFFF50057) // Hot Pink
        else -> Color.Gray
    }

    val typeIcon = when (file.fileType.lowercase()) {
        "mcworld" -> Icons.Filled.Language
        "mcaddon" -> Icons.Filled.Extension
        "mcpack" -> Icons.Filled.Layers
        else -> Icons.Filled.Description
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = if (selected) MinecraftGreen.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.05f),
                shape = RoundedCornerShape(24.dp)
            )
            .testTag("file_item_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) ObsidianSurfaceVariant else ObsidianSurface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Checkbox multi-selector
            Checkbox(
                checked = selected,
                onCheckedChange = onCheckedChange,
                colors = CheckboxDefaults.colors(
                    checkedColor = MinecraftGreen,
                    checkmarkColor = ObsidianBg,
                    uncheckedColor = TextMuted
                ),
                modifier = Modifier.testTag("file_item_checkbox")
            )

            Spacer(modifier = Modifier.width(4.dp))

            // Immersive Custom Icon Container
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        if (selected) MinecraftGreenVariant else colorIdentity.copy(alpha = 0.12f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = typeIcon,
                    contentDescription = file.fileType,
                    tint = if (selected) MinecraftGreen else colorIdentity,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // File descriptions
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = file.name,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    if (file.isDemo) {
                        Badge(
                            containerColor = ObsidianSurfaceVariant,
                            contentColor = GoldYellow,
                            modifier = Modifier.padding(horizontal = 2.dp)
                        ) {
                            Text(local.demoLabel, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    if (file.isImported) {
                        Badge(
                            containerColor = MinecraftGreenVariant.copy(alpha = 0.3f),
                            contentColor = MinecraftGreen,
                            modifier = Modifier.padding(horizontal = 2.dp)
                        ) {
                            Text(local.fileInfoLabel, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = ".${file.fileType.lowercase()} • ${formatFileSize(file.fileSize)}",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = (-0.2).sp
                    )
                }
            }

            // Quick Actions Block
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Favorite click action
                IconButton(
                    onClick = onFavoriteToggle,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("file_item_favorite")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = "Favorite",
                        tint = if (file.isFavorite) GoldYellow else TextMuted.copy(alpha = 0.25f),
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Individual install click action
                IconButton(
                    onClick = onExportClick,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Share,
                        contentDescription = "Install Pack",
                        tint = MinecraftGreen,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // File deleting click action
                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = "Delete",
                        tint = RedstoneRed.copy(alpha = 0.75f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(
    viewModel: McaViewModel,
    storageFolderUri: String?,
    minecraftInstalled: Boolean,
    local: Localization,
    onTriggerPicker: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .verticalScroll(scrollState)
    ) {
        Spacer(modifier = Modifier.height(28.dp))
        // Settings Header
        Text(
            text = local.settingsTitle,
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-0.5).sp,
            modifier = Modifier.testTag("settings_title")
        )
        Spacer(modifier = Modifier.height(20.dp))

        // Rule 1: Change application language (English and Portuguese)
        SettingsCard(
            title = local.settingsLanguageTitle,
            description = local.settingsLanguageDesc,
            icon = Icons.Filled.Language
        ) {
            val appLang = viewModel.currentLanguage.collectAsStateWithLifecycle().value
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { viewModel.setLanguage(AppLanguage.EN) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (appLang == AppLanguage.EN) MinecraftGreenVariant else ObsidianSurfaceVariant
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("language_selector_en"),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (appLang == AppLanguage.EN) MinecraftGreen.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.03f)
                    )
                ) {
                    Text(
                        text = "🇺🇸 English",
                        color = if (appLang == AppLanguage.EN) MinecraftGreen else TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
                Button(
                    onClick = { viewModel.setLanguage(AppLanguage.PT) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (appLang == AppLanguage.PT) MinecraftGreenVariant else ObsidianSurfaceVariant
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("language_selector_pt"),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (appLang == AppLanguage.PT) MinecraftGreen.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.03f)
                    )
                ) {
                    Text(
                        text = "🇧🇷 Português",
                        color = if (appLang == AppLanguage.PT) MinecraftGreen else TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Rule 2: Change application storage location
        SettingsCard(
            title = local.settingsStorageTitle,
            description = local.settingsStorageDesc,
            icon = Icons.Filled.FolderOpen
        ) {
            Spacer(modifier = Modifier.height(4.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = ObsidianSurfaceVariant
            ) {
                Text(
                    text = storageFolderUri?.let { Uri.parse(it).path ?: it } ?: local.settingsStorageNotSet,
                    color = if (storageFolderUri != null) MinecraftGreen else TextMuted,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(12.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onTriggerPicker,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("storage_folder_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = MinecraftGreen),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Filled.Edit, contentDescription = null, tint = ObsidianBg, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        if (viewModel.currentLanguage.value == AppLanguage.PT) "Alterar Pasta" else "Change Folder",
                        color = ObsidianBg,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (storageFolderUri != null) {
                    Button(
                        onClick = { viewModel.clearFolderAndReset() },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = RedstoneRed),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(local.resetStorageButton, color = ObsidianBg, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Rule 3: Locate the installed Minecraft application
        SettingsCard(
            title = local.settingsMinecraftTitle,
            description = local.settingsMinecraftDoc,
            icon = Icons.Filled.VideogameAsset
        ) {
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        if (minecraftInstalled) MinecraftGreenVariant.copy(alpha = 0.2f)
                        else RedstoneRed.copy(alpha = 0.15f)
                    )
                    .border(
                        width = 1.dp,
                        color = if (minecraftInstalled) MinecraftGreen else RedstoneRed.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(14.dp)
                    )
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (minecraftInstalled) Icons.Filled.CheckCircle else Icons.Filled.Warning,
                    contentDescription = null,
                    tint = if (minecraftInstalled) MinecraftGreen else RedstoneRed,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (minecraftInstalled) local.settingsMinecraftDetected else local.settingsMinecraftNotDetected,
                        color = if (minecraftInstalled) MinecraftGreen else TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Button(
                    onClick = {
                        viewModel.checkMinecraftInstallation()
                        viewModel.showStatus(
                            if (viewModel.currentLanguage.value == AppLanguage.PT) "Status verificado!" else "Installation checked successfully!"
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ObsidianSurfaceVariant),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("locate_minecraft_button")
                ) {
                    Icon(Icons.Filled.Refresh, contentDescription = "Retry check", tint = MinecraftGreen, modifier = Modifier.size(14.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Rule 4: View application information
        SettingsCard(
            title = local.settingsAppInfoTitle,
            description = local.settingsAppInfoDesc,
            icon = Icons.Filled.Info
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = { viewModel.setAppInfoDialogVisible(true) },
                colors = ButtonDefaults.buttonColors(containerColor = ObsidianSurfaceVariant),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("view_app_info_button"),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.FileOpen, contentDescription = null, tint = MinecraftGreen)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (viewModel.currentLanguage.value == AppLanguage.PT) "Visualizar Detalhes" else "View System Details",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Made by Lua Creative 🌙",
                color = GoldYellow.copy(alpha = 0.9f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }

        Spacer(modifier = Modifier.height(100.dp))
    }
}

@Composable
fun SettingsCard(
    title: String,
    description: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MinecraftGreen.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = MinecraftGreen, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Text(description, color = TextSecondary, fontSize = 12.sp, lineHeight = 16.sp)
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
            content()
        }
    }
}

@Composable
fun AppInfoDialog(
    local: Localization,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ObsidianSurfaceDialog,
        titleContentColor = TextPrimary,
        textContentColor = TextSecondary,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Info, contentDescription = null, tint = MinecraftGreen)
                Spacer(modifier = Modifier.width(10.dp))
                Text(local.aboutTitle, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = local.aboutVersion,
                    fontWeight = FontWeight.Bold,
                    color = GoldYellow,
                    fontSize = 14.sp
                )
                Text(
                    text = local.aboutDescription,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
                Divider(color = ObsidianSurfaceVariant, thickness = 1.dp)
                Text(
                    text = "Made by Lua Creative 🌙",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = GoldYellow
                )
                Text(
                    text = local.aboutCredits,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MinecraftGreen
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = MinecraftGreen),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("close_dialog_button")
            ) {
                Text(local.closeButton, color = ObsidianBg, fontWeight = FontWeight.Bold)
            }
        }
    )
}

// Custom format helper for filesizes
fun formatFileSize(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val exp = (Math.log(bytes.toDouble()) / Math.log(1024.0)).toInt()
    val pre = "KMGTPE"[exp - 1]
    return String.format("%.1f %sB", bytes / Math.pow(1024.0, exp.toDouble()), pre)
}

@Composable
fun AddonsScreen(
    viewModel: McaViewModel,
    local: Localization
) {
    var activeUrl by remember { mutableStateOf<String?>(null) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var pageProgress by remember { mutableStateOf(100) }
    var browserTitle by remember { mutableStateOf("") }

    if (activeUrl != null) {
        // WebView Browser Screen
        BackHandler {
            if (webViewRef?.canGoBack() == true) {
                webViewRef?.goBack()
            } else {
                activeUrl = null
            }
        }

        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Browser toolbar following modern design themes
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { activeUrl = null },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(ObsidianSurface)
                        .border(1.dp, Color.White.copy(alpha = 0.05f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = browserTitle.ifEmpty { "Loading..." },
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = activeUrl ?: "",
                        color = TextMuted,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))
                
                // Navigation keys
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = { webViewRef?.goBack() },
                        enabled = webViewRef?.canGoBack() == true,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Page Back",
                            tint = if (webViewRef?.canGoBack() == true) MinecraftGreen else TextMuted
                        )
                    }
                    IconButton(
                        onClick = { webViewRef?.goForward() },
                        enabled = webViewRef?.canGoForward() == true,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ArrowForward,
                            contentDescription = "Page Forward",
                            tint = if (webViewRef?.canGoForward() == true) MinecraftGreen else TextMuted
                        )
                    }
                    IconButton(
                        onClick = { webViewRef?.reload() },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = "Reload",
                            tint = MinecraftGreen
                        )
                    }
                }
            }

            // Progress bar
            AnimatedVisibility(visible = pageProgress < 100) {
                LinearProgressIndicator(
                    progress = { pageProgress / 100f },
                    color = MinecraftGreen,
                    trackColor = ObsidianSurface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                )
            }

            // WebView wrapped beautifully inside Jetpack's AndroidView
            val context = LocalContext.current
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.loadWithOverviewMode = true
                        settings.useWideViewPort = true
                        settings.databaseEnabled = true
                        settings.allowFileAccess = true
                        
                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                                super.onPageStarted(view, url, favicon)
                                url?.let { activeUrl = it }
                            }
                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                url?.let { activeUrl = it }
                                browserTitle = view?.title ?: ""
                            }
                            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                request?.url?.let { view?.loadUrl(it.toString()) }
                                return true
                            }
                        }
                        
                        webChromeClient = object : android.webkit.WebChromeClient() {
                            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                pageProgress = newProgress
                            }
                        }

                        setDownloadListener { url, userAgent, contentDisposition, mimetype, contentLength ->
                            viewModel.downloadFileToSelectedFolder(url, userAgent, contentDisposition, mimetype)
                        }

                        loadUrl(activeUrl ?: "")
                        webViewRef = this
                    }
                },
                update = { webView ->
                    if (webView.url != activeUrl) {
                        activeUrl?.let { webView.loadUrl(it) }
                    }
                    webViewRef = webView
                },
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
                    .background(Color.White)
            )
        }
    } else {
        // Portal Selection Screen
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(28.dp))
            Text(
                text = local.addonsTitle,
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.5).sp,
                modifier = Modifier.testTag("addons_screen_title")
            )
            Text(
                text = local.addonsDesc,
                color = TextMuted,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
            Spacer(modifier = Modifier.height(24.dp))

            // Scrollable list of 3 buttons
            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                AddonSourceCard(
                    title = "CurseForge Bedrock",
                    subtitle = if (local.language == AppLanguage.PT) "Explore mods, complementos e mapas oficiais do Bedrock" else "Explore official Bedrock mods, addons, and templates",
                    accentColor = Color(0xFFFF6B00),
                    url = "https://www.curseforge.com/minecraft-bedrock",
                    onClick = { activeUrl = "https://www.curseforge.com/minecraft-bedrock" }
                )

                AddonSourceCard(
                    title = "MCPEDL.com",
                    subtitle = if (local.language == AppLanguage.PT) "Busque mundos, skins, pacotes de texturas e comportamentos" else "Browse worlds, skins, behaviors, and custom resource packs",
                    accentColor = MinecraftGreen,
                    url = "https://mcpedl.com/",
                    onClick = { activeUrl = "https://mcpedl.com/" }
                )

                AddonSourceCard(
                    title = "Vatonage Addons",
                    subtitle = if (local.language == AppLanguage.PT) "Instale sistemas de comportamentos e mecânicas premium da comunidade" else "Acquire premium-grade behavior scripts and mechanics",
                    accentColor = Color(0xFFE91E63),
                    url = "https://vatonage.com/",
                    onClick = { activeUrl = "https://vatonage.com/" }
                )
                
                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }
}

@Composable
fun AddonSourceCard(
    title: String,
    subtitle: String,
    accentColor: Color,
    url: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = accentColor.copy(alpha = 0.25f),
                shape = RoundedCornerShape(24.dp)
            )
            .clickable(onClick = onClick)
            .testTag("addon_source_card_${title.lowercase().replace(" ", "_")}"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = ObsidianSurface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Modern Visual Indicator Dot representing company branding accent colors
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Language,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(18.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Filled.ArrowForward,
                        contentDescription = null,
                        tint = accentColor.copy(alpha = 0.6f),
                        modifier = Modifier.size(14.dp)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    color = TextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = url,
                    color = accentColor.copy(alpha = 0.8f),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = (-0.2).sp
                )
            }
        }
    }
}
