package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import com.example.ui.theme.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import android.content.ClipboardManager
import android.content.ClipData
import android.content.Context
import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.border
import kotlinx.coroutines.launch
import com.example.data.AiImportUiState
import com.example.data.AiVocabDraft
import com.example.data.Chapter
import com.example.data.VocabItem
import com.example.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val chapters by viewModel.allChapters.collectAsState()
    val selectedChapterId by viewModel.selectedChapterId.collectAsState()
    val items by viewModel.itemsForSelectedChapter.collectAsState()
    val exposedCount by viewModel.exposedItemsCount.collectAsState()
    val intervalMinutes by viewModel.intervalMinutes.collectAsState()
    val cycleMode by viewModel.cycleMode.collectAsState()
    val simulatedItem by viewModel.simulatedItem.collectAsState()
    val simulatedChapterName by viewModel.simulatedChapterName.collectAsState()
    val isScheduleModeEnabled by viewModel.isScheduleModeEnabled.collectAsState()
    val dayChapterAssignments by viewModel.dayChapterAssignments.collectAsState()

    val geminiKey by viewModel.geminiKey.collectAsState()
    val showApiKeyDialog by viewModel.showApiKeyDialog.collectAsState()
    val aiImportState by viewModel.aiImportState.collectAsState()
    val aiTitle by viewModel.aiTitle.collectAsState()
    val aiItems by viewModel.aiItems.collectAsState()

    var currentTab by rememberSaveable { mutableStateOf(0) } // 0 = Chapters, 1 = Items, 2 = Schedule, 3 = Widget Config
    
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var showAiImport by rememberSaveable { mutableStateOf(false) }

    // Search & Settings State
    var isSearchActive by rememberSaveable { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var showSettingsDialog by remember { mutableStateOf(false) }

    // Share / Import State
    var exportChapterName by remember { mutableStateOf<String?>(null) }
    var exportedShareCode by remember { mutableStateOf<String?>(null) }

    // Dialog Triggers
    var showAddChapterDialog by remember { mutableStateOf(false) }
    var showAddItemDialog by remember { mutableStateOf(false) }
    var itemToEdit by remember { mutableStateOf<VocabItem?>(null) }

    val activeChapterName = chapters.find { it.id == selectedChapterId }?.name ?: "No Chapter Selected"

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            if (isSearchActive) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            isSearchActive = false
                            searchQuery = ""
                        },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search Japanese words...", fontSize = 14.sp) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.outline
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Clear",
                                        tint = MaterialTheme.colorScheme.outline
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        ),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("top_search_input")
                    )
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Book,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Text(
                            text = "Komorebi",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = (-0.5).sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(
                            onClick = { isSearchActive = true },
                            modifier = Modifier.size(36.dp).testTag("top_search_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        IconButton(
                            onClick = { showSettingsDialog = true },
                            modifier = Modifier.size(36.dp).testTag("top_settings_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                NavigationBarItem(
                    selected = currentTab == 0,
                    onClick = { currentTab = 0 },
                    icon = { Icon(Icons.Outlined.Folder, contentDescription = "Chapters") },
                    label = { Text("Chapters") },
                    modifier = Modifier.testTag("tab_chapters")
                )
                NavigationBarItem(
                    selected = currentTab == 1,
                    onClick = { currentTab = 1 },
                    icon = { Icon(Icons.Outlined.Book, contentDescription = "Vocabulary") },
                    label = { Text("Items") },
                    modifier = Modifier.testTag("tab_items")
                )
                NavigationBarItem(
                    selected = currentTab == 2,
                    onClick = { currentTab = 2 },
                    icon = { Icon(Icons.Outlined.CalendarMonth, contentDescription = "Schedule") },
                    label = { Text("Schedule") },
                    modifier = Modifier.testTag("tab_schedule")
                )
                NavigationBarItem(
                    selected = currentTab == 3,
                    onClick = { currentTab = 3 },
                    icon = { Icon(Icons.Outlined.Widgets, contentDescription = "Widget Config") },
                    label = { Text("Widget") },
                    modifier = Modifier.testTag("tab_widget")
                )
            }
        },
        floatingActionButton = {
            if (currentTab == 0) {
                FloatingActionButton(
                    onClick = { showAddChapterDialog = true },
                    containerColor = SleekFabBg,
                    contentColor = SleekFabIcon,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.testTag("add_chapter_fab")
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Add Chapter", modifier = Modifier.size(28.dp))
                }
            } else if (currentTab == 1 && selectedChapterId != null) {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FloatingActionButton(
                        onClick = { showAddItemDialog = true },
                        containerColor = SleekFabBg,
                        contentColor = SleekFabIcon,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.testTag("add_item_fab")
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = "Add Vocabulary", modifier = Modifier.size(28.dp))
                    }
                    ExtendedFloatingActionButton(
                        onClick = { showAiImport = true },
                        containerColor = SleekFabBg,
                        contentColor = SleekFabIcon,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.testTag("ai_import_fab")
                    ) {
                        Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Scan List", fontSize = 13.sp)
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            when (currentTab) {
                0 -> ChaptersTabContent(
                    chapters = chapters,
                    selectedChapterId = selectedChapterId,
                    searchQuery = searchQuery,
                    onSelectChapter = { id ->
                        viewModel.selectChapter(id)
                        currentTab = 1 // Smoothly jump to items when chapter is clicked
                    },
                    onToggleExposure = { viewModel.toggleChapterExposure(it) },
                    onDeleteChapter = { viewModel.deleteChapter(it) },
                    onExportChapter = { chapter ->
                        scope.launch {
                            val code = viewModel.exportChapter(chapter.id)
                            if (code.isNotEmpty()) {
                                exportedShareCode = code
                                exportChapterName = chapter.name
                            } else {
                                Toast.makeText(context, "Error exporting chapter.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                )
                1 -> ItemsTabContent(
                    items = items,
                    activeChapterName = activeChapterName,
                    chapters = chapters,
                    selectedChapterId = selectedChapterId,
                    searchQuery = searchQuery,
                    onSelectChapter = { viewModel.selectChapter(it) },
                    onToggleExposure = { viewModel.toggleItemExposure(it) },
                    onEditItem = { itemToEdit = it },
                    onDeleteItem = { viewModel.deleteItem(it) }
                )
                2 -> ScheduleTabContent(
                    chapters = chapters,
                    isScheduleEnabled = isScheduleModeEnabled,
                    assignments = dayChapterAssignments,
                    onToggleSchedule = { viewModel.setScheduleModeEnabled(it) },
                    onAssign = { day, chId -> viewModel.assignChapterToDay(day, chId) }
                )
                3 -> WidgetSettingsTabContent(
                    exposedCount = exposedCount,
                    intervalMinutes = intervalMinutes,
                    cycleMode = cycleMode,
                    simulatedItem = simulatedItem,
                    simulatedChapterName = simulatedChapterName,
                    onIntervalChange = { viewModel.updateInterval(it) },
                    onCycleModeChange = { viewModel.updateCycleMode(it) },
                    onForceCycle = { viewModel.triggerForceCycle() }
                )
            }
        }
    }

    // Dialog: Add Chapter
    if (showAddChapterDialog) {
        AddChapterDialog(
            onDismiss = { showAddChapterDialog = false },
            onConfirm = { name ->
                viewModel.addChapter(name)
                showAddChapterDialog = false
            },
            onImportConfirm = { shareCode, onResult ->
                scope.launch {
                    val success = viewModel.importChapter(shareCode)
                    onResult(success)
                    if (success) {
                        Toast.makeText(context, "Chapter imported successfully!", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }

    // Dialog: Export Chapter (Share)
    if (exportedShareCode != null && exportChapterName != null) {
        ExportChapterDialog(
            chapterName = exportChapterName!!,
            shareCode = exportedShareCode!!,
            onDismiss = {
                exportedShareCode = null
                exportChapterName = null
            }
        )
    }

    // Dialog: Add Item
    if (showAddItemDialog) {
        AddEditItemDialog(
            title = "Add New Word",
            onDismiss = { showAddItemDialog = false },
            onConfirm = { word, reading, meaning, type, notes, exampleSentence ->
                viewModel.addItem(word, reading, meaning, type, notes, exampleSentence)
                showAddItemDialog = false
            }
        )
    }

    // Dialog: Edit Item
    itemToEdit?.let { item ->
        AddEditItemDialog(
            title = "Edit Word Details",
            initialWord = item.word,
            initialReading = item.reading,
            initialMeaning = item.meaning,
            initialType = item.type,
            initialNotes = item.notes,
            initialExampleSentence = item.exampleSentence,
            onDismiss = { itemToEdit = null },
            onConfirm = { word, reading, meaning, type, notes, exampleSentence ->
                viewModel.updateItem(item.copy(word = word, reading = reading, meaning = meaning, type = type, notes = notes, exampleSentence = exampleSentence))
                itemToEdit = null
            }
        )
    }

    if (showSettingsDialog) {
        SettingsDialog(
            onDismiss = { showSettingsDialog = false },
            hasApiKey = geminiKey != null,
            onManageApiKey = { viewModel.requestApiKeySetup() },
            onPreload = {
                viewModel.preloadSampleData()
                showSettingsDialog = false
            },
            onReset = {
                viewModel.resetDatabase()
                showSettingsDialog = false
            }
        )
    }

    if (showApiKeyDialog) {
        GeminiKeyDialog(
            onDismiss = { viewModel.dismissApiKeyDialog() },
            onConfirm = { viewModel.saveGeminiApiKey(it) }
        )
    }

    if (showAiImport) {
        AiImportScreen(
            geminiKey = geminiKey,
            state = aiImportState,
            aiTitle = aiTitle,
            aiItems = aiItems,
            onSaveApiKey = { viewModel.saveGeminiApiKey(it) },
            onExtract = { uri -> viewModel.startAiImport(uri) },
            onRetry = { viewModel.retryAiImport() },
            onTitleChange = viewModel::setAiTitle,
            onUpdateItem = viewModel::updateAiItem,
            onToggleItem = viewModel::toggleAiItem,
            onRemoveItem = viewModel::removeAiItem,
            onClose = {
                showAiImport = false
                viewModel.resetAiImport()
            },
            onCreateChapter = {
                scope.launch {
                    val chapterCreated = viewModel.createChapterFromAi()
                    if (chapterCreated) {
                        showAiImport = false
                        currentTab = 1
                        Toast.makeText(context, "Chapter created from your photo!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Select at least one entry to import.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }
}

// ==========================================
// 1. CHAPTERS TAB
// ==========================================
@Composable
fun ChaptersTabContent(
    chapters: List<Chapter>,
    selectedChapterId: Int?,
    searchQuery: String = "",
    onSelectChapter: (Int) -> Unit,
    onToggleExposure: (Chapter) -> Unit,
    onDeleteChapter: (Chapter) -> Unit,
    onExportChapter: (Chapter) -> Unit
) {
    val filteredChapters = remember(chapters, searchQuery) {
        if (searchQuery.isBlank()) {
            chapters
        } else {
            chapters.filter { it.name.contains(searchQuery, ignoreCase = true) }
        }
    }

    if (chapters.isEmpty()) {
        EmptyStateView(
            icon = Icons.Outlined.FolderOpen,
            title = "No Chapters Yet",
            description = "Chapters let you segment vocabularies (e.g. 'Genki Ch 1', 'N5 Verbs'). Tap the '+' button below to create your first chapter."
        )
    } else if (filteredChapters.isEmpty()) {
        EmptyStateView(
            icon = Icons.Outlined.SearchOff,
            title = "No Chapters Found",
            description = "No chapters matched your search query '$searchQuery'. Try checking for typos or searching something else."
        )
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = if (searchQuery.isBlank()) "Manage Chapters" else "Search Results",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            items(filteredChapters, key = { it.id }) { chapter ->
                val isSelected = chapter.id == selectedChapterId
                ChapterCard(
                    chapter = chapter,
                    isSelected = isSelected,
                    onSelect = { onSelectChapter(chapter.id) },
                    onToggleExposure = { onToggleExposure(chapter) },
                    onDelete = { onDeleteChapter(chapter) },
                    onExport = { onExportChapter(chapter) }
                )
            }
        }
    }
}

@Composable
fun ChapterCard(
    chapter: Chapter,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onToggleExposure: () -> Unit,
    onDelete: () -> Unit,
    onExport: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val isExposed = chapter.isExposed
    val containerColor = if (isExposed) {
        MaterialTheme.colorScheme.surfaceVariant
    } else {
        Color.White
    }

    val borderStroke = when {
        isSelected -> BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
        !isExposed -> BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        else -> null
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() }
            .testTag("chapter_card_${chapter.id}"),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = borderStroke,
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Folder Icon Box
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isExposed) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.background
                        }
                    )
                    .then(
                        if (!isExposed) {
                            Modifier.background(MaterialTheme.colorScheme.background)
                        } else {
                            Modifier
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.FolderOpen,
                    contentDescription = null,
                    tint = if (isExposed) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Text info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = chapter.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (isExposed) "Exposed & Active" else "Paused & Hidden",
                    fontSize = 12.sp,
                    color = if (isExposed) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }

            // Action Buttons Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Export Button
                IconButton(
                    onClick = onExport,
                    modifier = Modifier.size(40.dp).testTag("chapter_export_button_${chapter.id}")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Share,
                        contentDescription = "Share Chapter",
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Exposure toggle icon (visibility)
                IconButton(
                    onClick = onToggleExposure,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = if (isExposed) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                        contentDescription = "Toggle Exposure",
                        tint = if (isExposed) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        },
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Delete Button
                IconButton(
                    onClick = { showDeleteConfirm = true },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = "Delete Chapter",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Chapter?") },
            text = { Text("Deleting '${chapter.name}' will also delete all of its vocabulary and Kanji items. This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteConfirm = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// ==========================================
// 2. VOCAB & KANJI ITEMS TAB
// ==========================================
@Composable
fun ItemsTabContent(
    items: List<VocabItem>,
    activeChapterName: String,
    chapters: List<Chapter>,
    selectedChapterId: Int?,
    searchQuery: String = "",
    onSelectChapter: (Int) -> Unit,
    onToggleExposure: (VocabItem) -> Unit,
    onEditItem: (VocabItem) -> Unit,
    onDeleteItem: (VocabItem) -> Unit
) {
    var selectedTypeFilter by remember { mutableStateOf("all") } // "all", "vocab", "kanji"
    var expandedDropdown by remember { mutableStateOf(false) }

    if (chapters.isEmpty()) {
        EmptyStateView(
            icon = Icons.Outlined.Folder,
            title = "Create a Chapter First",
            description = "You need a chapter folder before you can start adding vocabulary or Kanji lists."
        )
        return
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Chapter selector bar
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expandedDropdown = true }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Selected Chapter", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                    Text(
                        text = activeChapterName,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Icon(Icons.Filled.ArrowDropDown, contentDescription = "Choose Chapter")
            }

            DropdownMenu(
                expanded = expandedDropdown,
                onDismissRequest = { expandedDropdown = false },
                modifier = Modifier.fillMaxWidth(0.9f)
            ) {
                chapters.forEach { chap ->
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (chap.isExposed) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = null,
                                    tint = if (chap.isExposed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(chap.name)
                            }
                        },
                        onClick = {
                            onSelectChapter(chap.id)
                            expandedDropdown = false
                        }
                    )
                }
            }
        }

        // Segmented filters
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterTabButton(
                text = "All (${items.size})",
                selected = selectedTypeFilter == "all",
                onClick = { selectedTypeFilter = "all" },
                modifier = Modifier.weight(1f)
            )
            FilterTabButton(
                text = "Vocab (${items.count { it.type == "vocab" }})",
                selected = selectedTypeFilter == "vocab",
                onClick = { selectedTypeFilter = "vocab" },
                modifier = Modifier.weight(1f)
            )
            FilterTabButton(
                text = "Kanji (${items.count { it.type == "kanji" }})",
                selected = selectedTypeFilter == "kanji",
                onClick = { selectedTypeFilter = "kanji" },
                modifier = Modifier.weight(1f)
            )
        }

        val filteredItems = remember(items, searchQuery, selectedTypeFilter) {
            val typeFiltered = items.filter {
                when (selectedTypeFilter) {
                    "vocab" -> it.type == "vocab"
                    "kanji" -> it.type == "kanji"
                    else -> true
                }
            }
            if (searchQuery.isBlank()) {
                typeFiltered
            } else {
                typeFiltered.filter {
                    it.word.contains(searchQuery, ignoreCase = true) ||
                    it.reading.contains(searchQuery, ignoreCase = true) ||
                    it.meaning.contains(searchQuery, ignoreCase = true) ||
                    it.notes.contains(searchQuery, ignoreCase = true)
                }
            }
        }

        if (items.isEmpty()) {
            EmptyStateView(
                icon = Icons.Outlined.Spellcheck,
                title = "No Words Yet",
                description = "Tap the '+' button below to add custom words, hiragana reading, and translation definitions to this chapter."
            )
        } else if (filteredItems.isEmpty()) {
            EmptyStateView(
                icon = Icons.Outlined.SearchOff,
                title = "No Matching Words",
                description = "We couldn't find any words matching '$searchQuery' in this chapter. Try searching something else."
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 80.dp, top = 8.dp, start = 16.dp, end = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredItems, key = { it.id }) { item ->
                    VocabItemRow(
                        item = item,
                        onToggleExposure = { onToggleExposure(item) },
                        onEdit = { onEditItem(item) },
                        onDelete = { onDeleteItem(item) }
                    )
                }
            }
        }
    }
}

@Composable
fun FilterTabButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .height(36.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(18.dp),
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Text(text = text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun VocabItemRow(
    item: VocabItem,
    onToggleExposure: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { isExpanded = !isExpanded }
            .testTag("item_row_${item.id}"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Word type marker
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(
                            if (item.type == "kanji") {
                                MaterialTheme.colorScheme.tertiaryContainer
                            } else {
                                MaterialTheme.colorScheme.secondaryContainer
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (item.type == "kanji") "漢" else "語",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (item.type == "kanji") {
                            MaterialTheme.colorScheme.onTertiaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSecondaryContainer
                        }
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    // Reading in smaller grey/primary
                    Text(
                        text = item.reading,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                    // Core word / Kanji
                    Text(
                        text = item.word,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Exposed check
                    IconButton(
                        onClick = onToggleExposure,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = if (item.isExposed) Icons.Filled.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                            contentDescription = "Toggle exposure",
                            tint = if (item.isExposed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                        )
                    }

                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = "Expand info",
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Expanded detail drawer
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp, start = 48.dp, end = 8.dp)
                ) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(bottom = 8.dp))

                    Text(
                        text = "Meaning",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.outline,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = item.meaning,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    if (item.notes.isNotBlank()) {
                        Text(
                            text = "Notes",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.outline,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = item.notes,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    if (item.exampleSentence.isNotBlank()) {
                        Text(
                            text = "Example Sentence",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.outline,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = item.exampleSentence,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = onEdit,
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Edit")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        TextButton(
                            onClick = onDelete,
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Delete")
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 3. WIDGET SETTINGS & LIVE LOCKSCREEN PREVIEW
// ==========================================
@Composable
fun WidgetSettingsTabContent(
    exposedCount: Int,
    intervalMinutes: Int,
    cycleMode: String,
    simulatedItem: VocabItem?,
    simulatedChapterName: String?,
    onIntervalChange: (Int) -> Unit,
    onCycleModeChange: (String) -> Unit,
    onForceCycle: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Lockscreen Simulation",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Live preview of what is currently exposed under your device clock.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }

        // Simulated Lock Screen Widget in Sleek Interface Design
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("lockscreen_simulation_widget"),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = SleekWidgetBg),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    // Top header row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = SleekWidgetAccent,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "ACTIVE WIDGET",
                                color = Color(0xFFCAC4D0),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 1.5.sp
                            )
                        }
                        Text(
                            text = "14:42",
                            color = Color(0xFFCAC4D0),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Normal
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Simulated word details centered
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        if (simulatedItem != null) {
                            Text(
                                text = simulatedItem.word,
                                fontSize = 40.sp,
                                fontWeight = FontWeight.Light,
                                color = Color.White,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "${simulatedItem.reading.uppercase()} • ${simulatedItem.meaning.uppercase()}",
                                color = SleekWidgetAccent,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                letterSpacing = 0.7.sp
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Box(
                                modifier = Modifier
                                    .height(1.dp)
                                    .width(48.dp)
                                    .background(Color.White.copy(alpha = 0.2f))
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = simulatedChapterName?.let { "Chapter: $it" } ?: "Chapter: Exposed List",
                                color = SleekInactive,
                                fontSize = 11.sp,
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                            )
                        } else {
                            Text(
                                text = "学習",
                                fontSize = 40.sp,
                                fontWeight = FontWeight.Light,
                                color = Color.White.copy(alpha = 0.35f),
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "GAKUSHŪ • STUDY",
                                color = SleekWidgetAccent.copy(alpha = 0.5f),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                letterSpacing = 0.7.sp
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Box(
                                modifier = Modifier
                                    .height(1.dp)
                                    .width(48.dp)
                                    .background(Color.White.copy(alpha = 0.15f))
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No active words exposed",
                                color = SleekInactive.copy(alpha = 0.6f),
                                fontSize = 11.sp,
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }

        // Widget Config Details
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Exposed Vocabulary Pool",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Items currently exposed:",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ) {
                            Text(
                                text = "$exposedCount words",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        // Interval Setting
        item {
            Text(
                text = "Rotation Time Interval",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Choose how frequently the word cycles on the widget.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            val intervals = listOf(
                Pair(1, "1 Min"),
                Pair(15, "15 Min"),
                Pair(60, "1 Hr"),
                Pair(240, "4 Hr"),
                Pair(720, "12 Hr"),
                Pair(1440, "24 Hr")
            )

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                intervals.chunked(2).forEach { pairList ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        pairList.forEach { (mins, label) ->
                            val isSelected = mins == intervalMinutes
                            OutlinedButton(
                                onClick = { onIntervalChange(mins) },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                    contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                ),
                                border = ButtonDefaults.outlinedButtonBorder.copy(
                                    brush = Brush.linearGradient(
                                        colors = if (isSelected) {
                                            listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)
                                        } else {
                                            listOf(MaterialTheme.colorScheme.outlineVariant, MaterialTheme.colorScheme.outlineVariant)
                                        }
                                    )
                                )
                            ) {
                                Text(label, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }

        // Cycle Order Mode
        item {
            Text(
                text = "Rotation Mode",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Choose whether cards cycle sequentially or randomly.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { onCycleModeChange("sequential") },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (cycleMode == "sequential") MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                        contentColor = if (cycleMode == "sequential") MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Icon(Icons.Default.CompareArrows, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Sequential")
                }

                OutlinedButton(
                    onClick = { onCycleModeChange("random") },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (cycleMode == "random") MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                        contentColor = if (cycleMode == "random") MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Icon(Icons.Default.Shuffle, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Random")
                }
            }
        }

        // Force Manual Rotate Button
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onForceCycle,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("force_rotate_button"),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.Loop, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Rotate Now", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// ==========================================
// SHARED GENERAL COMPOSABLES
// ==========================================
@Composable
fun EmptyStateView(
    icon: ImageVector,
    title: String,
    description: String
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = description,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.outline,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(0.9f)
            )
        }
    }
}

@Composable
fun AddChapterDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    onImportConfirm: (String, (Boolean) -> Unit) -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) } // 0 = Create, 1 = Import
    var name by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = if (selectedTab == 0) "Add New Chapter" else "Import Shared Chapter",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // Segmented control or choice buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterTabButton(
                        text = "New Chapter",
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        modifier = Modifier.weight(1f)
                    )
                    FilterTabButton(
                        text = "Import Code",
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        modifier = Modifier.weight(1f)
                    )
                }

                if (selectedTab == 0) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = {
                            name = it
                            if (it.isNotBlank()) isError = false
                        },
                        label = { Text("Chapter Name") },
                        placeholder = { Text("e.g. Genki Chapter 1") },
                        isError = isError,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("dialog_chapter_input")
                    )

                    if (isError) {
                        Text(
                            text = "Name cannot be blank.",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text("Cancel")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (name.isNotBlank()) {
                                    onConfirm(name.trim())
                                } else {
                                    isError = true
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier.testTag("dialog_chapter_confirm")
                        ) {
                            Text("Create")
                        }
                    }
                } else {
                    var shareCode by remember { mutableStateOf("") }
                    var importError by remember { mutableStateOf<String?>(null) }
                    val scope = rememberCoroutineScope()

                    Column {
                        Text(
                            text = "Paste the shared chapter code below to import it.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        OutlinedTextField(
                            value = shareCode,
                            onValueChange = {
                                shareCode = it
                                importError = null
                            },
                            label = { Text("Shared Chapter Code") },
                            placeholder = { Text("Paste long code here...") },
                            isError = importError != null,
                            maxLines = 5,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .testTag("dialog_chapter_import_input")
                        )

                        if (importError != null) {
                            Text(
                                text = importError ?: "",
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = onDismiss) {
                                Text("Cancel")
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    if (shareCode.trim().isBlank()) {
                                        importError = "Code cannot be empty."
                                    } else {
                                        onImportConfirm(shareCode.trim()) { success ->
                                            if (success) {
                                                onDismiss()
                                            } else {
                                                importError = "Invalid code. Please make sure you copied it correctly."
                                            }
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                modifier = Modifier.testTag("dialog_chapter_import_confirm")
                            ) {
                                Text("Import")
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditItemDialog(
    title: String,
    initialWord: String = "",
    initialReading: String = "",
    initialMeaning: String = "",
    initialType: String = "vocab",
    initialNotes: String = "",
    initialExampleSentence: String = "",
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, String, String, String) -> Unit
) {
    var word by remember { mutableStateOf(initialWord) }
    var reading by remember { mutableStateOf(initialReading) }
    var meaning by remember { mutableStateOf(initialMeaning) }
    var type by remember { mutableStateOf(initialType) } // "vocab" or "kanji"
    var notes by remember { mutableStateOf(initialNotes) }
    var exampleSentence by remember { mutableStateOf(initialExampleSentence) }

    var isWordError by remember { mutableStateOf(false) }
    var isReadingError by remember { mutableStateOf(false) }
    var isMeaningError by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .heightIn(max = 620.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // Type selector: Vocab vs Kanji
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterTabButton(
                        text = "Vocab",
                        selected = type == "vocab",
                        onClick = { type = "vocab" },
                        modifier = Modifier.weight(1f)
                    )
                    FilterTabButton(
                        text = "Kanji",
                        selected = type == "kanji",
                        onClick = { type = "kanji" },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Core Japanese text (Kanji / Word)
                OutlinedTextField(
                    value = word,
                    onValueChange = {
                        word = it
                        if (it.isNotBlank()) isWordError = false
                    },
                    label = { Text(if (type == "kanji") "Kanji Word" else "Vocabulary Word") },
                    placeholder = { Text(if (type == "kanji") "e.g. 日" else "e.g. 日本語") },
                    isError = isWordError,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("dialog_item_word_input")
                )
                if (isWordError) {
                    Text("Word cannot be empty", color = MaterialTheme.colorScheme.error, fontSize = 11.sp)
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Hiragana/Katakana Reading
                OutlinedTextField(
                    value = reading,
                    onValueChange = {
                        reading = it
                        if (it.isNotBlank()) isReadingError = false
                    },
                    label = { Text("Reading (Kana)") },
                    placeholder = { Text(if (type == "kanji") "e.g. ひ / にち" else "e.g. にほんご") },
                    isError = isReadingError,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("dialog_item_reading_input")
                )
                if (isReadingError) {
                    Text("Reading cannot be empty", color = MaterialTheme.colorScheme.error, fontSize = 11.sp)
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Meaning (English)
                OutlinedTextField(
                    value = meaning,
                    onValueChange = {
                        meaning = it
                        if (it.isNotBlank()) isMeaningError = false
                    },
                    label = { Text("Translation / Meaning") },
                    placeholder = { Text("e.g. Day / Sun" + if (type == "kanji") "" else " or Japanese language") },
                    isError = isMeaningError,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("dialog_item_meaning_input")
                )
                if (isMeaningError) {
                    Text("Meaning cannot be empty", color = MaterialTheme.colorScheme.error, fontSize = 11.sp)
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Additional Notes (Optional)
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (Optional)") },
                    placeholder = { Text("e.g. Ru-verb, used for eating.") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth().testTag("dialog_item_notes_input")
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Example Sentence (Optional)
                OutlinedTextField(
                    value = exampleSentence,
                    onValueChange = { exampleSentence = it },
                    label = { Text("Example Sentence (Optional)") },
                    placeholder = { Text("e.g. ご飯を食べる。") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth().testTag("dialog_item_example_input")
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            var hasError = false
                            if (word.isBlank()) { isWordError = true; hasError = true }
                            if (reading.isBlank()) { isReadingError = true; hasError = true }
                            if (meaning.isBlank()) { isMeaningError = true; hasError = true }

                            if (!hasError) {
                                onConfirm(word.trim(), reading.trim(), meaning.trim(), type, notes.trim(), exampleSentence.trim())
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.testTag("dialog_item_confirm")
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsDialog(
    onDismiss: () -> Unit,
    hasApiKey: Boolean,
    onManageApiKey: () -> Unit,
    onPreload: () -> Unit,
    onReset: () -> Unit
) {
    var showResetConfirm by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .testTag("settings_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Title
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "App Settings",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Scrollable content
                LazyColumn(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // About
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "Komorebi v1.1.0",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "A passive learning tool that cycles custom Japanese vocabularies & Kanji on your home screen or lock screen widgets.",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }

                    // Gemini AI
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Gemini AI Import",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (hasApiKey) {
                                        "API key set. You can scan vocabulary lists from a photo."
                                    } else {
                                        "No API key yet. AI photo import stays locked until you add one."
                                    },
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 16.sp
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = onManageApiKey,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(
                                        imageVector = if (hasApiKey) Icons.Default.Edit else Icons.Default.Add,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(if (hasApiKey) "Change API Key" else "Add API Key", fontSize = 13.sp)
                                }
                            }
                        }
                    }

                    // How to use widget
                    item {
                        Column {
                            Text(
                                text = "How to Setup Widget",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "1. Go to your Android Home Screen.\n" +
                                       "2. Press and hold on an empty space.\n" +
                                       "3. Select 'Widgets' (or 'Add widgets').\n" +
                                       "4. Search or scroll to find 'Komorebi'.\n" +
                                       "5. Drag the widget onto your home screen or lockscreen slot.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 18.sp
                            )
                        }
                    }

                    // SQLite & Data management
                    item {
                        Column {
                            Text(
                                text = "Database & Seed Utilities",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Manage the local SQLite database storing your Japanese word lists.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )

                            // Preload Button
                            Button(
                                onClick = onPreload,
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Load Sample Vocab", fontSize = 13.sp)
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Reset Button
                            OutlinedButton(
                                onClick = { showResetConfirm = true },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Reset Data", fontSize = 13.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Footer Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Close")
                    }
                }
            }
        }
    }

    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text("Reset Database?") },
            text = { Text("Are you sure you want to completely erase your Japanese vocabulary database? This action is permanent and will delete all custom lists, chapters, and items.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onReset()
                        showResetConfirm = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Reset Everything")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun ExportChapterDialog(
    chapterName: String,
    shareCode: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = remember { context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("export_chapter_dialog"),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Share Chapter",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Share your vocabulary lists of '$chapterName' with your friends using this compact share code.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Scrollable Box for share code
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = shareCode,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.verticalScroll(rememberScrollState())
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Close")
                    }

                    Button(
                        onClick = {
                            val clipData = ClipData.newPlainText("Komorebi Chapter Share Code", shareCode)
                            clipboardManager.setPrimaryClip(clipData)
                            Toast.makeText(context, "Share code copied to clipboard!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Copy Code")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleTabContent(
    chapters: List<Chapter>,
    isScheduleEnabled: Boolean,
    assignments: Map<Int, Int>,
    onToggleSchedule: (Boolean) -> Unit,
    onAssign: (Int, Int) -> Unit
) {
    val daysOfWeek = remember {
        listOf(
            1 to "Monday",
            2 to "Tuesday",
            3 to "Wednesday",
            4 to "Thursday",
            5 to "Friday",
            6 to "Saturday",
            7 to "Sunday"
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Daily Schedule Planner",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Assign a specific chapter to each day of the week to automatically filter widget items.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        // Toggle Switch Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                modifier = Modifier.fillMaxWidth().testTag("schedule_mode_toggle_card")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = null,
                            tint = if (isScheduleEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                        )
                        Column {
                            Text(
                                text = "Schedule Mode",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (isScheduleEnabled) "Active (Filtering by Day)" else "Disabled (Rotating All Exposed)",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                    Switch(
                        checked = isScheduleEnabled,
                        onCheckedChange = onToggleSchedule,
                        modifier = Modifier.testTag("schedule_mode_switch")
                    )
                }
            }
        }

        item {
            Text(
                text = "Weekly Assignments",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }

        // 7 Days list
        items(daysOfWeek) { (dayNum, dayName) ->
            val assignedChapterId = assignments[dayNum] ?: -1
            val assignedChapter = chapters.find { it.id == assignedChapterId }
            var showDropdown by remember { mutableStateOf(false) }

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (assignedChapter != null && isScheduleEnabled) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    }
                ),
                border = BorderStroke(
                    1.dp,
                    if (assignedChapter != null && isScheduleEnabled) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                    } else {
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                    }
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable(enabled = isScheduleEnabled) { showDropdown = true }
                    .testTag("schedule_day_card_$dayNum")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        // Day initial badge
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isScheduleEnabled) {
                                        if (assignedChapter != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)
                                    } else {
                                        MaterialTheme.colorScheme.outlineVariant
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = dayName.take(1),
                                color = if (assignedChapter != null && isScheduleEnabled) Color.White else MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = dayName,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isScheduleEnabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            if (assignedChapter != null) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FolderOpen,
                                        contentDescription = null,
                                        tint = if (isScheduleEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = assignedChapter.name,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (isScheduleEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            } else {
                                Text(
                                    text = "All Exposed (Fallback)",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.outline,
                                    style = androidx.compose.ui.text.TextStyle(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                                )
                            }
                        }
                    }

                    // Dropdown for choosing chapter
                    Box {
                        IconButton(
                            onClick = { showDropdown = true },
                            enabled = isScheduleEnabled,
                            modifier = Modifier.testTag("day_assign_button_$dayNum")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Assign Chapter",
                                tint = if (isScheduleEnabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.outline
                            )
                        }

                        DropdownMenu(
                            expanded = showDropdown,
                            onDismissRequest = { showDropdown = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("None (rotate all)") },
                                onClick = {
                                    onAssign(dayNum, -1)
                                    showDropdown = false
                                }
                            )
                            HorizontalDivider()
                            chapters.forEach { chapter ->
                                DropdownMenuItem(
                                    text = { Text(chapter.name) },
                                    onClick = {
                                        onAssign(dayNum, chapter.id)
                                        showDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 4. GEMINI AI PHOTO IMPORT
// ==========================================
@Composable
fun GeminiKeyDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var key by remember { mutableStateOf("") }
    var showKey by remember { mutableStateOf(false) }
    var isError by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("api_key_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Gemini API Key",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Enter a free Google AI Studio API key to unlock photo import of vocab lists. " +
                        "The key is stored only on this device. You can skip this for now and add it later from Settings.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = key,
                    onValueChange = {
                        key = it
                        if (it.isNotBlank()) isError = false
                    },
                    label = { Text("API Key") },
                    placeholder = { Text("AIza...") },
                    isError = isError,
                    singleLine = true,
                    visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showKey = !showKey }) {
                            Icon(
                                imageVector = if (showKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (showKey) "Hide key" else "Show key",
                                tint = MaterialTheme.colorScheme.outline
                            )
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    keyboardActions = KeyboardActions(onDone = {
                        if (key.trim().length >= 20) onConfirm(key.trim())
                    }),
                    modifier = Modifier.fillMaxWidth().testTag("api_key_input")
                )
                if (isError) {
                    Text(
                        text = "Please paste a valid key.",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Not now")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (key.trim().length >= 20) {
                                onConfirm(key.trim())
                            } else {
                                isError = true
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.testTag("api_key_save_button")
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}

@Composable
fun AiImportScreen(
    geminiKey: String?,
    state: AiImportUiState,
    aiTitle: String,
    aiItems: List<AiVocabDraft>,
    onSaveApiKey: (String) -> Unit,
    onExtract: (Uri) -> Unit,
    onRetry: () -> Unit,
    onTitleChange: (String) -> Unit,
    onUpdateItem: (Int, AiVocabDraft) -> Unit,
    onToggleItem: (Int) -> Unit,
    onRemoveItem: (Int) -> Unit,
    onCreateChapter: () -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    var pickedUri by remember { mutableStateOf<Uri?>(null) }
    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var itemToEdit by remember { mutableStateOf<Pair<Int, AiVocabDraft>?>(null) }
    var showKeyDialog by remember { mutableStateOf(false) }

    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            pickedUri = uri
            previewBitmap = context.contentResolver.decodePreviewBitmap(uri)
        }
    }

    BackHandler(enabled = true, onBack = onClose)

    Dialog(onDismissRequest = onClose) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "AI Photo Import",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    IconButton(onClick = onClose) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                when {
                    state is AiImportUiState.Loading -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator()
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Extracting vocabulary...",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }

                    state is AiImportUiState.Error -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Outlined.ErrorOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Extraction failed",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = state.message,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            Button(onClick = onRetry) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Try Again")
                            }
                            TextButton(onClick = onClose) { Text("Cancel") }
                        }
                    }

                    aiItems.isNotEmpty() -> {
                        Column(modifier = Modifier.fillMaxSize()) {
                            OutlinedTextField(
                                value = aiTitle,
                                onValueChange = onTitleChange,
                                label = { Text("Chapter Title") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                                    .testTag("ai_title_input")
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Review the entries below. Uncheck any you don't want, or tap edit to fix details.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                            LazyColumn(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                itemsIndexed(aiItems, key = { i, _ -> i }) { index, draft ->
                                    Card(
                                        modifier = Modifier.fillMaxWidth().testTag("ai_item_row_$index"),
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(start = 4.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Checkbox(
                                                checked = draft.selected,
                                                onCheckedChange = { onToggleItem(index) },
                                                modifier = Modifier.testTag("ai_item_check_$index")
                                            )
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = draft.word,
                                                    fontSize = 16.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                if (draft.reading.isNotBlank() || draft.meaning.isNotBlank()) {
                                                    Text(
                                                        text = buildString {
                                                            if (draft.reading.isNotBlank()) append(draft.reading)
                                                            if (draft.reading.isNotBlank() && draft.meaning.isNotBlank()) append(" • ")
                                                            if (draft.meaning.isNotBlank()) append(draft.meaning)
                                                        },
                                                        fontSize = 12.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        maxLines = 2,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            }
                                            IconButton(onClick = { itemToEdit = index to draft }) {
                                                Icon(
                                                    Icons.Default.Edit,
                                                    contentDescription = "Edit entry",
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                            IconButton(onClick = { onRemoveItem(index) }) {
                                                Icon(
                                                    Icons.Default.Delete,
                                                    contentDescription = "Remove entry",
                                                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            Surface(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .windowInsetsPadding(WindowInsets.navigationBars)
                                        .padding(16.dp)
                                ) {
                                    Text(
                                        text = "${aiItems.count { it.selected }} of ${aiItems.size} selected",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Row {
                                        OutlinedButton(
                                            onClick = {
                                                pickedUri = null
                                                previewBitmap = null
                                            },
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Text("New Photo")
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Button(
                                            onClick = onCreateChapter,
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(48.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                            shape = RoundedCornerShape(12.dp),
                                            enabled = aiItems.any { it.selected }
                                        ) {
                                            Icon(
                                                Icons.Default.Add,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Create Chapter", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    else -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 32.dp, vertical = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Scan a photo of a Japanese vocab list",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Pick an image, let the AI extract each entry, then review, edit, and turn it into a chapter.",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.outline,
                                textAlign = TextAlign.Center,
                                lineHeight = 18.sp
                            )
                            Spacer(modifier = Modifier.height(24.dp))

                            if (previewBitmap != null) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(220.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                ) {
                                    Image(
                                        bitmap = previewBitmap!!.asImageBitmap(),
                                        contentDescription = "Selected photo",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Fit
                                    )
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(160.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            Icons.Outlined.Image,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.outline,
                                            modifier = Modifier.size(40.dp)
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "No photo selected",
                                            color = MaterialTheme.colorScheme.outline,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                            }

                            Button(
                                onClick = {
                                    imagePicker.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Choose Photo", fontSize = 14.sp)
                            }

                            if (previewBitmap != null && pickedUri != null) {
                                Spacer(modifier = Modifier.height(12.dp))
                                if (geminiKey == null) {
                                    OutlinedButton(
                                        onClick = { showKeyDialog = true },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(48.dp),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Add Gemini API Key", fontSize = 14.sp)
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Extraction is locked until an API key is added.",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.outline,
                                        textAlign = TextAlign.Center
                                    )
                                } else {
                                    Button(
                                        onClick = { onExtract(pickedUri!!) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(48.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.tertiary
                                        ),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Extract Words", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            if (geminiKey == null && previewBitmap == null) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "AI needs a Gemini API key to read photos. You can skip it and enable this feature later from Settings.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.outline,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showKeyDialog) {
        GeminiKeyDialog(
            onDismiss = { showKeyDialog = false },
            onConfirm = {
                onSaveApiKey(it)
                showKeyDialog = false
            }
        )
    }

    itemToEdit?.let { (index, draft) ->
        AddEditItemDialog(
            title = "Edit Extracted Entry",
            initialWord = draft.word,
            initialReading = draft.reading,
            initialMeaning = draft.meaning,
            initialType = draft.type,
            initialNotes = draft.notes,
            initialExampleSentence = draft.example,
            onDismiss = { itemToEdit = null },
            onConfirm = { word, reading, meaning, type, notes, exampleSentence ->
                onUpdateItem(
                    index,
                    draft.copy(
                        word = word,
                        reading = reading,
                        meaning = meaning,
                        type = type,
                        notes = notes,
                        example = exampleSentence
                    )
                )
                itemToEdit = null
            }
        )
    }
}

private fun ContentResolver.decodePreviewBitmap(uri: Uri, maxDim: Int = 1600): Bitmap? {
    return try {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        val largest = maxOf(bounds.outWidth, bounds.outHeight)
        if (largest <= 0) return null
        var sampleSize = 1
        while (largest / (sampleSize * 2) >= maxDim) sampleSize *= 2
        val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
    } catch (e: Exception) {
        null
    }
}

