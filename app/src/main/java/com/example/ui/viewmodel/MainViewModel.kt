package com.example.ui.viewmodel

import android.app.Application
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.AppRepository
import com.example.data.Chapter
import com.example.data.VocabItem
import com.example.widget.VocabWidgetProvider
import com.example.widget.WidgetScheduleHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: AppRepository
    private val prefs = application.getSharedPreferences("widget_prefs", Context.MODE_PRIVATE)

    val allChapters: StateFlow<List<Chapter>>
    val exposedItemsCount: StateFlow<Int>

    private val _selectedChapterId = MutableStateFlow<Int?>(null)
    val selectedChapterId: StateFlow<Int?> = _selectedChapterId.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val itemsForSelectedChapter: StateFlow<List<VocabItem>> = _selectedChapterId
        .flatMapLatest { id ->
            if (id == null) {
                flowOf(emptyList())
            } else {
                repository.getItemsForChapter(id)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Widget Preferences
    private val _intervalMinutes = MutableStateFlow(WidgetScheduleHelper.getIntervalMinutes(application))
    val intervalMinutes: StateFlow<Int> = _intervalMinutes.asStateFlow()

    private val _cycleMode = MutableStateFlow(prefs.getString("cycle_mode", "sequential") ?: "sequential")
    val cycleMode: StateFlow<String> = _cycleMode.asStateFlow()

    // Live Simulated Widget State
    private val _simulatedItem = MutableStateFlow<VocabItem?>(null)
    val simulatedItem: StateFlow<VocabItem?> = _simulatedItem.asStateFlow()

    init {
        val appDao = AppDatabase.getDatabase(application).appDao()
        repository = AppRepository(appDao)

        allChapters = repository.allChapters.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        exposedItemsCount = repository.exposedItemsCount.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

        // Select the first chapter by default when loaded
        viewModelScope.launch {
            allChapters.collect { chapters ->
                if (_selectedChapterId.value == null && chapters.isNotEmpty()) {
                    _selectedChapterId.value = chapters.first().id
                }
            }
        }

        // Periodically refresh preview when elements change
        viewModelScope.launch {
            repository.allItems.collect {
                refreshSimulatedPreview()
            }
        }
    }

    fun selectChapter(chapterId: Int) {
        _selectedChapterId.value = chapterId
    }

    // Chapters
    fun addChapter(name: String) = viewModelScope.launch(Dispatchers.IO) {
        val chapterId = repository.insertChapter(Chapter(name = name))
        if (_selectedChapterId.value == null) {
            _selectedChapterId.value = chapterId.toInt()
        }
    }

    fun deleteChapter(chapter: Chapter) = viewModelScope.launch(Dispatchers.IO) {
        repository.deleteChapter(chapter)
        if (_selectedChapterId.value == chapter.id) {
            _selectedChapterId.value = allChapters.value.firstOrNull { it.id != chapter.id }?.id
        }
        notifyWidgetUpdate()
    }

    fun toggleChapterExposure(chapter: Chapter) = viewModelScope.launch(Dispatchers.IO) {
        repository.updateChapter(chapter.copy(isExposed = !chapter.isExposed))
        notifyWidgetUpdate()
    }

    // Items (Vocab / Kanji)
    fun addItem(word: String, reading: String, meaning: String, type: String, notes: String = "") = 
        viewModelScope.launch(Dispatchers.IO) {
            val chapterId = _selectedChapterId.value ?: return@launch
            repository.insertItem(
                VocabItem(
                    chapterId = chapterId,
                    word = word,
                    reading = reading,
                    meaning = meaning,
                    type = type,
                    notes = notes
                )
            )
            notifyWidgetUpdate()
        }

    fun updateItem(item: VocabItem) = viewModelScope.launch(Dispatchers.IO) {
        repository.updateItem(item)
        notifyWidgetUpdate()
    }

    fun deleteItem(item: VocabItem) = viewModelScope.launch(Dispatchers.IO) {
        repository.deleteItem(item)
        notifyWidgetUpdate()
    }

    fun toggleItemExposure(item: VocabItem) = viewModelScope.launch(Dispatchers.IO) {
        repository.updateItem(item.copy(isExposed = !item.isExposed))
        notifyWidgetUpdate()
    }

    // Settings
    fun updateInterval(minutes: Int) {
        _intervalMinutes.value = minutes
        WidgetScheduleHelper.setIntervalMinutes(getApplication(), minutes)
    }

    fun updateCycleMode(mode: String) {
        _cycleMode.value = mode
        prefs.edit().putString("cycle_mode", mode).apply()
        notifyWidgetUpdate()
    }

    // Force systems to select the next word and redraw the active widgets immediately
    fun triggerForceCycle() = viewModelScope.launch {
        val context = getApplication<Application>()
        val intent = Intent(context, VocabWidgetProvider::class.java).apply {
            action = VocabWidgetProvider.ACTION_MANUAL_REFRESH
        }
        context.sendBroadcast(intent)
        
        // Wait briefly for disk writes to complete, then refresh live preview
        kotlinx.coroutines.delay(100)
        refreshSimulatedPreview()
    }

    fun refreshSimulatedPreview() = viewModelScope.launch(Dispatchers.IO) {
        val exposed = repository.getExposedItems()
        if (exposed.isEmpty()) {
            _simulatedItem.value = null
        } else {
            val currentId = prefs.getInt("current_vocab_id", -1)
            var active = exposed.find { it.id == currentId }
            if (active == null) {
                active = exposed.first()
            }
            _simulatedItem.value = active
        }
    }

    private fun notifyWidgetUpdate() {
        val context = getApplication<Application>()
        val intent = Intent(context, VocabWidgetProvider::class.java).apply {
            action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
        }
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val componentName = ComponentName(context, VocabWidgetProvider::class.java)
        val ids = appWidgetManager.getAppWidgetIds(componentName)
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
        context.sendBroadcast(intent)
        
        refreshSimulatedPreview()
    }
}
