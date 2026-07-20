package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import com.example.ui.theme.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
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

    var currentTab by remember { mutableStateOf(0) } // 0 = Chapters, 1 = Items, 2 = Widget Config
    
    // Dialog Triggers
    var showAddChapterDialog by remember { mutableStateOf(false) }
    var showAddItemDialog by remember { mutableStateOf(false) }
    var itemToEdit by remember { mutableStateOf<VocabItem?>(null) }

    val activeChapterName = chapters.find { it.id == selectedChapterId }?.name ?: "No Chapter Selected"

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
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
                        onClick = { /* Search action */ },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(
                        onClick = { /* Settings action */ },
                        modifier = Modifier.size(36.dp)
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
                    label = { Text("Vocab & Kanji") },
                    modifier = Modifier.testTag("tab_items")
                )
                NavigationBarItem(
                    selected = currentTab == 2,
                    onClick = { currentTab = 2 },
                    icon = { Icon(Icons.Outlined.Widgets, contentDescription = "Widget Config") },
                    label = { Text("Lockscreen") },
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
                FloatingActionButton(
                    onClick = { showAddItemDialog = true },
                    containerColor = SleekFabBg,
                    contentColor = SleekFabIcon,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.testTag("add_item_fab")
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Add Vocabulary", modifier = Modifier.size(28.dp))
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
                    onSelectChapter = { id ->
                        viewModel.selectChapter(id)
                        currentTab = 1 // Smoothly jump to items when chapter is clicked
                    },
                    onToggleExposure = { viewModel.toggleChapterExposure(it) },
                    onDeleteChapter = { viewModel.deleteChapter(it) }
                )
                1 -> ItemsTabContent(
                    items = items,
                    activeChapterName = activeChapterName,
                    chapters = chapters,
                    selectedChapterId = selectedChapterId,
                    onSelectChapter = { viewModel.selectChapter(it) },
                    onToggleExposure = { viewModel.toggleItemExposure(it) },
                    onEditItem = { itemToEdit = it },
                    onDeleteItem = { viewModel.deleteItem(it) }
                )
                2 -> WidgetSettingsTabContent(
                    exposedCount = exposedCount,
                    intervalMinutes = intervalMinutes,
                    cycleMode = cycleMode,
                    simulatedItem = simulatedItem,
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
            }
        )
    }

    // Dialog: Add Item
    if (showAddItemDialog) {
        AddEditItemDialog(
            title = "Add New Word",
            onDismiss = { showAddItemDialog = false },
            onConfirm = { word, reading, meaning, type, notes ->
                viewModel.addItem(word, reading, meaning, type, notes)
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
            onDismiss = { itemToEdit = null },
            onConfirm = { word, reading, meaning, type, notes ->
                viewModel.updateItem(item.copy(word = word, reading = reading, meaning = meaning, type = type, notes = notes))
                itemToEdit = null
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
    onSelectChapter: (Int) -> Unit,
    onToggleExposure: (Chapter) -> Unit,
    onDeleteChapter: (Chapter) -> Unit
) {
    if (chapters.isEmpty()) {
        EmptyStateView(
            icon = Icons.Outlined.FolderOpen,
            title = "No Chapters Yet",
            description = "Chapters let you segment vocabularies (e.g. 'Genki Ch 1', 'N5 Verbs'). Tap the '+' button below to create your first chapter."
        )
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = "Manage Chapters",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            items(chapters, key = { it.id }) { chapter ->
                val isSelected = chapter.id == selectedChapterId
                ChapterCard(
                    chapter = chapter,
                    isSelected = isSelected,
                    onSelect = { onSelectChapter(chapter.id) },
                    onToggleExposure = { onToggleExposure(chapter) },
                    onDelete = { onDeleteChapter(chapter) }
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
    onDelete: () -> Unit
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

        val filteredItems = items.filter {
            when (selectedTypeFilter) {
                "vocab" -> it.type == "vocab"
                "kanji" -> it.type == "kanji"
                else -> true
            }
        }

        if (filteredItems.isEmpty()) {
            EmptyStateView(
                icon = Icons.Outlined.Spellcheck,
                title = "No Words in Filter",
                description = "Tap the '+' button below to add custom words, hiragana reading, and translation definitions to this chapter."
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
                            text = "Notes / Sentences",
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
                                text = "Chapter: Live Exposure List",
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
                Pair(1, "1 Min (Test)"),
                Pair(15, "15 Min"),
                Pair(60, "1 Hour"),
                Pair(240, "4 Hours"),
                Pair(720, "12 Hours"),
                Pair(1440, "24 Hours")
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
                Text("Rotate Widget Card Now", fontSize = 15.sp, fontWeight = FontWeight.Bold)
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
    onConfirm: (String) -> Unit
) {
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
                    text = "Add New Chapter",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

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
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, String, String) -> Unit
) {
    var word by remember { mutableStateOf(initialWord) }
    var reading by remember { mutableStateOf(initialReading) }
    var meaning by remember { mutableStateOf(initialMeaning) }
    var type by remember { mutableStateOf(initialType) } // "vocab" or "kanji"
    var notes by remember { mutableStateOf(initialNotes) }

    var isWordError by remember { mutableStateOf(false) }
    var isReadingError by remember { mutableStateOf(false) }
    var isMeaningError by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
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
                        text = "Vocabulary (語)",
                        selected = type == "vocab",
                        onClick = { type = "vocab" },
                        modifier = Modifier.weight(1f)
                    )
                    FilterTabButton(
                        text = "Kanji (漢)",
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
                    label = { Text("Notes & Sentences (Optional)") },
                    placeholder = { Text("e.g. Used for counting days.") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth().testTag("dialog_item_notes_input")
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
                                onConfirm(word.trim(), reading.trim(), meaning.trim(), type, notes.trim())
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
