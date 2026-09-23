package com.example.ui.screens

import com.example.R
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
import androidx.compose.ui.res.stringResource
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
import android.app.Activity
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
import com.example.widget.WidgetThemes

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
    val widgetTheme by viewModel.widgetTheme.collectAsState()
    val simulatedItem by viewModel.simulatedItem.collectAsState()
    val simulatedChapterName by viewModel.simulatedChapterName.collectAsState()
    val isScheduleModeEnabled by viewModel.isScheduleModeEnabled.collectAsState()
    val dayChapterAssignments by viewModel.dayChapterAssignments.collectAsState()

    val geminiKey by viewModel.geminiKey.collectAsState()
    val showApiKeyDialog by viewModel.showApiKeyDialog.collectAsState()
    val aiImportState by viewModel.aiImportState.collectAsState()
    val aiTitle by viewModel.aiTitle.collectAsState()
    val aiItems by viewModel.aiItems.collectAsState()
    val appLanguage by viewModel.appLanguage.collectAsState()

    var currentTab by rememberSaveable { mutableStateOf(0) } // 0 = Chapters, 1 = Items, 2 = Schedule, 3 = Widget Config
    
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var showAiImport by rememberSaveable { mutableStateOf(false) }

    // Search & Settings State
    var isSearchActive by rememberSaveable { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }

    // Share / Import State
    var exportChapterName by remember { mutableStateOf<String?>(null) }
    var exportedShareCode by remember { mutableStateOf<String?>(null) }

    // Dialog Triggers
    var showAddChapterDialog by remember { mutableStateOf(false) }
    var showAddItemDialog by remember { mutableStateOf(false) }
    var itemToEdit by remember { mutableStateOf<VocabItem?>(null) }

    val activeChapterName = chapters.find { it.id == selectedChapterId }?.name ?: stringResource(R.string.no_chapter_selected)

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
                            contentDescription = stringResource(R.string.back),
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text(stringResource(R.string.search_hint), fontSize = 14.sp) },
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
                                        contentDescription = stringResource(R.string.clear),
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
                                contentDescription = stringResource(R.string.search),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        IconButton(
                            onClick = { showLanguageDialog = true },
                            modifier = Modifier.size(36.dp).testTag("top_language_button")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Translate,
                                contentDescription = stringResource(R.string.language_button),
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
                                contentDescription = stringResource(R.string.settings),
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
                    icon = { Icon(Icons.Outlined.Folder, contentDescription = stringResource(R.string.tab_chapters)) },
                    label = { Text(stringResource(R.string.tab_chapters)) },
                    modifier = Modifier.testTag("tab_chapters")
                )
                NavigationBarItem(
                    selected = currentTab == 1,
                    onClick = { currentTab = 1 },
                    icon = { Icon(Icons.Outlined.Book, contentDescription = stringResource(R.string.tab_items_icon)) },
                    label = { Text(stringResource(R.string.tab_items)) },
                    modifier = Modifier.testTag("tab_items")
                )
                NavigationBarItem(
                    selected = currentTab == 2,
                    onClick = { currentTab = 2 },
                    icon = { Icon(Icons.Outlined.CalendarMonth, contentDescription = stringResource(R.string.tab_schedule)) },
                    label = { Text(stringResource(R.string.tab_schedule)) },
                    modifier = Modifier.testTag("tab_schedule")
                )
                NavigationBarItem(
                    selected = currentTab == 3,
                    onClick = { currentTab = 3 },
                    icon = { Icon(Icons.Outlined.Widgets, contentDescription = stringResource(R.string.tab_widget_icon)) },
                    label = { Text(stringResource(R.string.tab_widget)) },
                    modifier = Modifier.testTag("tab_widget")
                )
            }
        },
        floatingActionButton = {
            if (currentTab == 1 && selectedChapterId != null) {
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
                        Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.add_vocabulary), modifier = Modifier.size(28.dp))
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
                        Text(stringResource(R.string.scan_list), fontSize = 13.sp)
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
                    onCreateChapter = { showAddChapterDialog = true },
                    onToggleExposure = { viewModel.toggleChapterExposure(it) },
                    onDeleteChapter = { viewModel.deleteChapter(it) },
                    onExportChapter = { chapter ->
                        scope.launch {
                            val code = viewModel.exportChapter(chapter.id)
                            if (code.isNotEmpty()) {
                                exportedShareCode = code
                                exportChapterName = chapter.name
                            } else {
                                Toast.makeText(context, context.getString(R.string.export_error), Toast.LENGTH_SHORT).show()
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
                    widgetTheme = widgetTheme,
                    simulatedItem = simulatedItem,
                    simulatedChapterName = simulatedChapterName,
                    onIntervalChange = { viewModel.updateInterval(it) },
                    onCycleModeChange = { viewModel.updateCycleMode(it) },
                    onWidgetThemeChange = { viewModel.updateWidgetTheme(it) },
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
            onScanAi = {
                showAddChapterDialog = false
                showAiImport = true
            },
            onImportConfirm = { shareCode, onResult ->
                scope.launch {
                    val success = viewModel.importChapter(shareCode)
                    onResult(success)
                    if (success) {
                        Toast.makeText(context, context.getString(R.string.chapter_imported), Toast.LENGTH_SHORT).show()
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
            title = stringResource(R.string.add_new_word),
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
            title = stringResource(R.string.edit_word_details),
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

    if (showLanguageDialog) {
        LanguageDialog(
            current = appLanguage,
            onSelect = { code ->
                viewModel.setAppLanguage(code)
                showLanguageDialog = false
                (context as? Activity)?.recreate()
            },
            onDismiss = { showLanguageDialog = false }
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
                        Toast.makeText(context, context.getString(R.string.chapter_created), Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, context.getString(R.string.select_entry_to_import), Toast.LENGTH_SHORT).show()
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
    onCreateChapter: () -> Unit,
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

    Box(modifier = Modifier.fillMaxSize()) {
        if (chapters.isEmpty()) {
            EmptyStateView(
                icon = Icons.Outlined.FolderOpen,
                title = stringResource(R.string.no_chapters_yet),
                description = stringResource(R.string.no_chapters_yet_hint)
            )
        } else if (filteredChapters.isEmpty()) {
            EmptyStateView(
                icon = Icons.Outlined.SearchOff,
                title = stringResource(R.string.no_chapters_found),
                description = stringResource(R.string.no_chapters_found_hint, searchQuery)
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        text = if (searchQuery.isBlank()) stringResource(R.string.manage_chapters) else stringResource(R.string.search_results),
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

        Button(
            onClick = onCreateChapter,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 16.dp)
                .fillMaxWidth()
                .height(56.dp)
                .testTag("create_chapter_cta"),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(containerColor = SleekFabBg, contentColor = SleekFabIcon)
        ) {
            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.new_chapter_cta), fontSize = 16.sp, fontWeight = FontWeight.Bold)
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
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
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
                    text = if (isExposed) stringResource(R.string.exposed_active) else stringResource(R.string.paused_hidden),
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
                        contentDescription = stringResource(R.string.share_chapter),
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
                        contentDescription = stringResource(R.string.toggle_exposure),
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
                        contentDescription = stringResource(R.string.delete_chapter),
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
            title = { Text(stringResource(R.string.delete_chapter_title)) },
            text = { Text(stringResource(R.string.delete_chapter_body, chapter.name)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteConfirm = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(stringResource(R.string.cancel))
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
            title = stringResource(R.string.create_chapter_first),
            description = stringResource(R.string.need_chapter_first)
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
                    Text(stringResource(R.string.selected_chapter), fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                    Text(
                        text = activeChapterName,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Icon(Icons.Filled.ArrowDropDown, contentDescription = stringResource(R.string.choose_chapter))
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
                text = stringResource(R.string.all_filter) + " (${items.size})",
                selected = selectedTypeFilter == "all",
                onClick = { selectedTypeFilter = "all" },
                modifier = Modifier.weight(1f)
            )
            FilterTabButton(
                text = stringResource(R.string.vocab_filter) + " (${items.count { it.type == "vocab" }})",
                selected = selectedTypeFilter == "vocab",
                onClick = { selectedTypeFilter = "vocab" },
                modifier = Modifier.weight(1f)
            )
            FilterTabButton(
                text = stringResource(R.string.kanji_filter) + " (${items.count { it.type == "kanji" }})",
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
                title = stringResource(R.string.no_words_yet),
                description = stringResource(R.string.no_words_hint)
            )
        } else if (filteredItems.isEmpty()) {
            EmptyStateView(
                icon = Icons.Outlined.SearchOff,
                title = stringResource(R.string.no_matching_words),
                description = stringResource(R.string.no_matching_hint, searchQuery)
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
                            contentDescription = stringResource(R.string.toggle_item_exposure),
                            tint = if (item.isExposed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                        )
                    }

                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = stringResource(R.string.expand_info),
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
                        text = stringResource(R.string.meaning),
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
                            text = stringResource(R.string.notes),
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
                            text = stringResource(R.string.example_sentence),
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
                            Text(stringResource(R.string.edit))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        TextButton(
                            onClick = onDelete,
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(R.string.delete))
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
    widgetTheme: String,
    simulatedItem: VocabItem?,
    simulatedChapterName: String?,
    onIntervalChange: (Int) -> Unit,
    onCycleModeChange: (String) -> Unit,
    onWidgetThemeChange: (String) -> Unit,
    onForceCycle: () -> Unit
) {
    val palette = WidgetThemes.byKey(widgetTheme)
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = stringResource(R.string.lockscreen_simulation),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(R.string.lockscreen_preview_hint),
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
                colors = CardDefaults.cardColors(containerColor = Color(palette.background)),
                border = BorderStroke(1.dp, Color(palette.chapter).copy(alpha = 0.5f)),
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
                                tint = Color(palette.reading),
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.active_widget),
                                color = Color(palette.chapter),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 1.5.sp
                            )
                        }
                        Text(
                            text = "14:42",
                            color = Color(palette.chapter),
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
                                color = Color(palette.word),
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "${simulatedItem.reading.uppercase()} • ${simulatedItem.meaning.uppercase()}",
                                color = Color(palette.reading),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                letterSpacing = 0.7.sp
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Box(
                                modifier = Modifier
                                    .height(1.dp)
                                    .width(48.dp)
                                    .background(Color(palette.chapter).copy(alpha = 0.4f))
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = simulatedChapterName?.let { stringResource(R.string.chapter_prefix) + it } ?: stringResource(R.string.chapter_exposed_list),
                                color = Color(palette.chapter),
                                fontSize = 11.sp,
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                            )
                        } else {
                            Text(
                                text = "学習",
                                fontSize = 40.sp,
                                fontWeight = FontWeight.Light,
                                color = Color(palette.word).copy(alpha = 0.35f),
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "GAKUSHŪ • STUDY",
                                color = Color(palette.reading).copy(alpha = 0.5f),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                letterSpacing = 0.7.sp
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Box(
                                modifier = Modifier
                                    .height(1.dp)
                                    .width(48.dp)
                                    .background(Color(palette.chapter).copy(alpha = 0.3f))
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = stringResource(R.string.no_active_words),
                                color = Color(palette.chapter).copy(alpha = 0.6f),
                                fontSize = 11.sp,
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }

        // Widget Theme
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.widget_theme),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.widget_theme_hint),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        WidgetThemes.all.forEach { theme ->
                            val isSelected = theme.key == widgetTheme
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    onClick = { onWidgetThemeChange(theme.key) },
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(theme.background),
                                    contentColor = Color(theme.reading),
                                    border = BorderStroke(
                                        if (isSelected) 2.dp else 1.dp,
                                        if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                                    ),
                                    modifier = Modifier
                                        .size(width = 56.dp, height = 64.dp)
                                        .testTag("widget_theme_${theme.key}")
                                ) {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        if (isSelected) {
                                            Icon(
                                                Icons.Default.Check,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        } else {
                                            Text("A", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = theme.key.replaceFirstChar { it.uppercase() },
                                    fontSize = 10.sp,
                                    color = if (isSelected) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.outline
                                    }
                                )
                            }
                        }
                    }
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
                        text = stringResource(R.string.exposed_pool),
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
                            text = stringResource(R.string.items_exposed),
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ) {
                            Text(
                                text = stringResource(R.string.exposed_words_count, exposedCount),
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
                text = stringResource(R.string.rotation_interval),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(R.string.rotation_interval_hint),
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
                text = stringResource(R.string.rotation_mode),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(R.string.rotation_mode_hint),
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
                    Text(stringResource(R.string.sequential))
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
                    Text(stringResource(R.string.random))
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
                Text(stringResource(R.string.rotate_now), fontSize = 15.sp, fontWeight = FontWeight.Bold)
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
    onImportConfirm: (String, (Boolean) -> Unit) -> Unit,
    onScanAi: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) } // 0 = Create, 1 = Import
    var name by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    val dialogContext = LocalContext.current

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
                    text = if (selectedTab == 0) stringResource(R.string.add_new_chapter) else stringResource(R.string.import_shared_chapter),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = onScanAi,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("scan_ai_pill"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.scan_ai_button), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Segmented control or choice buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterTabButton(
                        text = stringResource(R.string.new_chapter),
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        modifier = Modifier.weight(1f)
                    )
                    FilterTabButton(
                        text = stringResource(R.string.import_code),
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
                        label = { Text(stringResource(R.string.chapter_name_label)) },
                        placeholder = { Text(stringResource(R.string.chapter_name_hint)) },
                        isError = isError,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("dialog_chapter_input")
                    )

                    if (isError) {
                        Text(
                            text = stringResource(R.string.name_blank_error),
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
                            Text(stringResource(R.string.cancel))
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
                            Text(stringResource(R.string.create))
                        }
                    }
                } else {
                    var shareCode by remember { mutableStateOf("") }
                    var importError by remember { mutableStateOf<String?>(null) }
                    val scope = rememberCoroutineScope()

                    Column {
                        Text(
                            text = stringResource(R.string.import_code_hint),
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
                            label = { Text(stringResource(R.string.shared_code_label)) },
                            placeholder = { Text(stringResource(R.string.paste_code_hint)) },
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
                                Text(stringResource(R.string.cancel))
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    if (shareCode.trim().isBlank()) {
                                        importError = dialogContext.getString(R.string.code_empty_error)
                                    } else {
                                        onImportConfirm(shareCode.trim()) { success ->
                                            if (success) {
                                                onDismiss()
                                            } else {
                                                importError = dialogContext.getString(R.string.code_invalid_error)
                                            }
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                modifier = Modifier.testTag("dialog_chapter_import_confirm")
                            ) {
                                Text(stringResource(R.string.import_btn))
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
                        text = stringResource(R.string.vocab_type),
                        selected = type == "vocab",
                        onClick = { type = "vocab" },
                        modifier = Modifier.weight(1f)
                    )
                    FilterTabButton(
                        text = stringResource(R.string.kanji_type),
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
                    label = { Text(if (type == "kanji") stringResource(R.string.kanji_word_label) else stringResource(R.string.vocabulary_word_label)) },
                    placeholder = { Text(if (type == "kanji") stringResource(R.string.word_kanji_hint) else stringResource(R.string.word_vocab_hint)) },
                    isError = isWordError,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("dialog_item_word_input")
                )
                if (isWordError) {
                    Text(stringResource(R.string.word_empty_error), color = MaterialTheme.colorScheme.error, fontSize = 11.sp)
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Hiragana/Katakana Reading
                OutlinedTextField(
                    value = reading,
                    onValueChange = {
                        reading = it
                        if (it.isNotBlank()) isReadingError = false
                    },
                    label = { Text(stringResource(R.string.reading_label)) },
                    placeholder = { Text(if (type == "kanji") stringResource(R.string.reading_kanji_hint) else stringResource(R.string.reading_vocab_hint)) },
                    isError = isReadingError,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("dialog_item_reading_input")
                )
                if (isReadingError) {
                    Text(stringResource(R.string.reading_empty_error), color = MaterialTheme.colorScheme.error, fontSize = 11.sp)
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Meaning (English)
                OutlinedTextField(
                    value = meaning,
                    onValueChange = {
                        meaning = it
                        if (it.isNotBlank()) isMeaningError = false
                    },
                    label = { Text(stringResource(R.string.translation_label)) },
                    placeholder = { Text(stringResource(R.string.meaning_hint_start) + if (type == "kanji") "" else stringResource(R.string.meaning_hint_end)) },
                    isError = isMeaningError,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("dialog_item_meaning_input")
                )
                if (isMeaningError) {
                    Text(stringResource(R.string.meaning_empty_error), color = MaterialTheme.colorScheme.error, fontSize = 11.sp)
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Additional Notes (Optional)
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(stringResource(R.string.notes_optional)) },
                    placeholder = { Text(stringResource(R.string.notes_hint)) },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth().testTag("dialog_item_notes_input")
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Example Sentence (Optional)
                OutlinedTextField(
                    value = exampleSentence,
                    onValueChange = { exampleSentence = it },
                    label = { Text(stringResource(R.string.example_optional)) },
                    placeholder = { Text(stringResource(R.string.example_hint)) },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth().testTag("dialog_item_example_input")
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.cancel))
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
                        Text(stringResource(R.string.save))
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
                        text = stringResource(R.string.app_settings),
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
                                    text = stringResource(R.string.settings_version),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = stringResource(R.string.settings_about),
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
                                        text = stringResource(R.string.gemini_ai_import),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (hasApiKey) {
                                        stringResource(R.string.api_key_set)
                                    } else {
                                        stringResource(R.string.api_key_missing)
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
                                    Text(if (hasApiKey) stringResource(R.string.change_api_key) else stringResource(R.string.add_api_key), fontSize = 13.sp)
                                }
                            }
                        }
                    }

                    // How to use widget
                    item {
                        Column {
                            Text(
                                text = stringResource(R.string.how_to_setup_widget),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = stringResource(R.string.how_to_widget),
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
                                text = stringResource(R.string.db_seed_utilities),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(R.string.db_seed_description),
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
                                Text(stringResource(R.string.load_sample), fontSize = 13.sp)
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
                                Text(stringResource(R.string.reset_data), fontSize = 13.sp)
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
                        Text(stringResource(R.string.close))
                    }
                }
            }
        }
    }

    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text(stringResource(R.string.reset_db_title)) },
            text = { Text(stringResource(R.string.reset_db_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onReset()
                        showResetConfirm = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.reset_everything))
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) {
                    Text(stringResource(R.string.cancel))
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
                        text = stringResource(R.string.share_chapter),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = stringResource(R.string.export_share_body, chapterName),
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
                        Text(stringResource(R.string.close))
                    }

                    Button(
                        onClick = {
                            val clipData = ClipData.newPlainText("Komorebi Chapter Share Code", shareCode)
                            clipboardManager.setPrimaryClip(clipData)
                            Toast.makeText(context, context.getString(R.string.code_copied), Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.copy_code))
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
    val daysOfWeek = listOf(
        1 to stringResource(R.string.day_monday),
        2 to stringResource(R.string.day_tuesday),
        3 to stringResource(R.string.day_wednesday),
        4 to stringResource(R.string.day_thursday),
        5 to stringResource(R.string.day_friday),
        6 to stringResource(R.string.day_saturday),
        7 to stringResource(R.string.day_sunday)
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = stringResource(R.string.schedule_planner),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = stringResource(R.string.schedule_planner_hint),
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
                                text = stringResource(R.string.schedule_mode),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (isScheduleEnabled) stringResource(R.string.schedule_active) else stringResource(R.string.schedule_disabled),
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
                text = stringResource(R.string.weekly_assignments),
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
                                    text = stringResource(R.string.all_exposed_fallback),
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
                                contentDescription = stringResource(R.string.assign_chapter),
                                tint = if (isScheduleEnabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.outline
                            )
                        }

                        DropdownMenu(
                            expanded = showDropdown,
                            onDismissRequest = { showDropdown = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.none_rotate_all)) },
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
                        text = stringResource(R.string.gemini_api_key),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.gemini_key_desc),
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
                    label = { Text(stringResource(R.string.api_key_label)) },
                    placeholder = { Text(stringResource(R.string.ai_key_hint)) },
                    isError = isError,
                    singleLine = true,
                    visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showKey = !showKey }) {
                            Icon(
                                imageVector = if (showKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (showKey) stringResource(R.string.hide_key) else stringResource(R.string.show_key),
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
                        text = stringResource(R.string.key_invalid_error),
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
                        Text(stringResource(R.string.not_now))
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
                        Text(stringResource(R.string.save))
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
                            text = stringResource(R.string.ai_photo_import),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    IconButton(onClick = onClose) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = stringResource(R.string.close),
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
                                    text = stringResource(R.string.extracting_vocab),
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
                                text = stringResource(R.string.extraction_failed),
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
                                Text(stringResource(R.string.try_again))
                            }
                            TextButton(onClick = onClose) { Text(stringResource(R.string.cancel)) }
                        }
                    }

                    aiItems.isNotEmpty() -> {
                        Column(modifier = Modifier.fillMaxSize()) {
                            OutlinedTextField(
                                value = aiTitle,
                                onValueChange = onTitleChange,
                                label = { Text(stringResource(R.string.chapter_title_label)) },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                                    .testTag("ai_title_input")
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.review_entries_hint),
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
                                                    contentDescription = stringResource(R.string.edit_entry),
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                            IconButton(onClick = { onRemoveItem(index) }) {
                                                Icon(
                                                    Icons.Default.Delete,
                                                    contentDescription = stringResource(R.string.remove_entry),
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
                                        text = stringResource(R.string.selected_count, aiItems.count { it.selected }, aiItems.size),
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
                                            Text(stringResource(R.string.new_photo))
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
                                            Text(stringResource(R.string.create_chapter), fontSize = 14.sp, fontWeight = FontWeight.Bold)
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
                                text = stringResource(R.string.scan_photo_title),
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = stringResource(R.string.scan_photo_hint),
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
                                        contentDescription = stringResource(R.string.selected_photo),
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
                                            text = stringResource(R.string.no_photo_selected),
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
                                Text(stringResource(R.string.choose_photo), fontSize = 14.sp)
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
                                        Text(stringResource(R.string.add_gemini_api_key), fontSize = 14.sp)
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = stringResource(R.string.extraction_locked),
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
                                        Text(stringResource(R.string.extract_words), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            if (geminiKey == null && previewBitmap == null) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = stringResource(R.string.ai_needs_key),
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
            title = stringResource(R.string.edit_extracted_entry),
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

@Composable
fun LanguageDialog(
    current: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("language_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.Translate,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = stringResource(R.string.lang_section),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.lang_section_hint),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
                listOf("en" to R.string.english, "my" to R.string.burmese).forEach { (code, labelRes) ->
                    val selected = code == current
                    Surface(
                        onClick = { onSelect(code) },
                        shape = RoundedCornerShape(12.dp),
                        color = if (selected) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        },
                        contentColor = if (selected) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(stringResource(labelRes), fontSize = 15.sp, fontWeight = FontWeight.Medium)
                            if (selected) {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.close))
                    }
                }
            }
        }
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

