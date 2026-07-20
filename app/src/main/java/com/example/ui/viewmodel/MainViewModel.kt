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
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.json.JSONArray
import android.util.Base64

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

    // Schedule Mode State
    private val _isScheduleModeEnabled = MutableStateFlow(prefs.getBoolean("schedule_mode_enabled", false))
    val isScheduleModeEnabled: StateFlow<Boolean> = _isScheduleModeEnabled.asStateFlow()

    private val _dayChapterAssignments = MutableStateFlow<Map<Int, Int>>(loadDayChapterAssignments())
    val dayChapterAssignments: StateFlow<Map<Int, Int>> = _dayChapterAssignments.asStateFlow()

    private fun loadDayChapterAssignments(): Map<Int, Int> {
        val map = mutableMapOf<Int, Int>()
        for (day in 1..7) {
            map[day] = prefs.getInt("schedule_day_$day", -1)
        }
        return map
    }

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
    fun addItem(word: String, reading: String, meaning: String, type: String, notes: String = "", exampleSentence: String = "") = 
        viewModelScope.launch(Dispatchers.IO) {
            val chapterId = _selectedChapterId.value ?: return@launch
            repository.insertItem(
                VocabItem(
                    chapterId = chapterId,
                    word = word,
                    reading = reading,
                    meaning = meaning,
                    type = type,
                    notes = notes,
                    exampleSentence = exampleSentence
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

    fun setScheduleModeEnabled(enabled: Boolean) {
        _isScheduleModeEnabled.value = enabled
        prefs.edit().putBoolean("schedule_mode_enabled", enabled).apply()
        notifyWidgetUpdate()
    }

    fun assignChapterToDay(dayOfWeek: Int, chapterId: Int) {
        prefs.edit().putInt("schedule_day_$dayOfWeek", chapterId).apply()
        _dayChapterAssignments.value = loadDayChapterAssignments()
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

    fun resetDatabase() = viewModelScope.launch(Dispatchers.IO) {
        repository.deleteAllChapters()
        _selectedChapterId.value = null
        notifyWidgetUpdate()
    }

    fun preloadSampleData() = viewModelScope.launch(Dispatchers.IO) {
        // Clear first
        repository.deleteAllChapters()
        
        // 1. Create N5 Verbs Chapter
        val ch1Id = repository.insertChapter(Chapter(name = "N5 Essential Verbs", isExposed = true))
        repository.insertItem(VocabItem(chapterId = ch1Id.toInt(), word = "食べる", reading = "たべる", meaning = "to eat", type = "vocab", notes = "Ru-verb. Example: ご飯を食べる (eat a meal)"))
        repository.insertItem(VocabItem(chapterId = ch1Id.toInt(), word = "飲む", reading = "のむ", meaning = "to drink", type = "vocab", notes = "U-verb. Example: 水を飲む (drink water)"))
        repository.insertItem(VocabItem(chapterId = ch1Id.toInt(), word = "行く", reading = "いく", meaning = "to go", type = "vocab", notes = "U-verb. Example: 学校に行く (go to school)"))
        repository.insertItem(VocabItem(chapterId = ch1Id.toInt(), word = "見る", reading = "みる", meaning = "to see / watch", type = "vocab", notes = "Ru-verb. Example: テレビを見る (watch TV)"))
        repository.insertItem(VocabItem(chapterId = ch1Id.toInt(), word = "勉強する", reading = "べんきょうする", meaning = "to study", type = "vocab", notes = "Irregular verb. Example: 日本語を勉強する"))

        // 2. Create N5 Kanji Chapter
        val ch2Id = repository.insertChapter(Chapter(name = "JLPT N5 Kanji", isExposed = true))
        repository.insertItem(VocabItem(chapterId = ch2Id.toInt(), word = "一", reading = "いち", meaning = "one", type = "kanji", notes = "Kunyomi: ひと. Onyomi: イチ. Radical: 一"))
        repository.insertItem(VocabItem(chapterId = ch2Id.toInt(), word = "日", reading = "ひ", meaning = "day / sun / Japan", type = "kanji", notes = "Kunyomi: ひ. Onyomi: ニチ / ジツ"))
        repository.insertItem(VocabItem(chapterId = ch2Id.toInt(), word = "本", reading = "ほん", meaning = "book / origin", type = "kanji", notes = "Kunyomi: もと. Onyomi: ホン"))
        repository.insertItem(VocabItem(chapterId = ch2Id.toInt(), word = "木", reading = "き", meaning = "tree / wood", type = "kanji", notes = "Kunyomi: き. Onyomi: モク"))
        repository.insertItem(VocabItem(chapterId = ch2Id.toInt(), word = "人", reading = "ひと", meaning = "person / human", type = "kanji", notes = "Kunyomi: ひと. Onyomi: ジン / ニン"))

        // 3. Create Komorebi Favorites Chapter
        val ch3Id = repository.insertChapter(Chapter(name = "Komorebi Beautiful Words", isExposed = true))
        repository.insertItem(VocabItem(chapterId = ch3Id.toInt(), word = "木漏れ日", reading = "こもれび", meaning = "sunlight filtering through trees", type = "vocab", notes = "A poetic word that describes the interplay between light and leaves. Origin of this app's name!"))
        repository.insertItem(VocabItem(chapterId = ch3Id.toInt(), word = "桜", reading = "さくら", meaning = "cherry blossom", type = "vocab", notes = "Symbol of spring, beauty, and transience in Japanese culture."))
        repository.insertItem(VocabItem(chapterId = ch3Id.toInt(), word = "森林浴", reading = "しんりんよく", meaning = "forest bathing / therapeutic forest walk", type = "vocab", notes = "Relaxing in a forest environment to improve physical and mental health."))

        // Set active chapter to the first seeded one
        _selectedChapterId.value = ch1Id.toInt()
        notifyWidgetUpdate()
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

    // Returns a Base64-encoded shareable string representing the chapter and its items
    suspend fun exportChapter(chapterId: Int): String {
        return withContext(Dispatchers.IO) {
            try {
                val chapter = repository.getChapterById(chapterId) ?: return@withContext ""
                val items = repository.getItemsForChapterList(chapterId)
                
                val obj = JSONObject()
                obj.put("version", 1)
                obj.put("name", chapter.name)
                
                val array = JSONArray()
                for (item in items) {
                    val itemObj = JSONObject()
                    itemObj.put("word", item.word)
                    itemObj.put("reading", item.reading)
                    itemObj.put("meaning", item.meaning)
                    itemObj.put("type", item.type)
                    itemObj.put("notes", item.notes)
                    itemObj.put("exampleSentence", item.exampleSentence)
                    itemObj.put("isExposed", item.isExposed)
                    array.put(itemObj)
                }
                obj.put("items", array)
                
                val jsonStr = obj.toString()
                Base64.encodeToString(jsonStr.toByteArray(Charsets.UTF_8), Base64.NO_WRAP or Base64.URL_SAFE)
            } catch (e: Exception) {
                e.printStackTrace()
                ""
            }
        }
    }

    // Decodes the code and imports the chapter. Returns true if successful.
    suspend fun importChapter(shareCode: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val decodedBytes = Base64.decode(shareCode.trim(), Base64.NO_WRAP or Base64.URL_SAFE)
                val jsonStr = String(decodedBytes, Charsets.UTF_8)
                val obj = JSONObject(jsonStr)
                
                val name = obj.getString("name")
                val chapterId = repository.insertChapter(Chapter(name = name, isExposed = true))
                
                val itemsArray = obj.getJSONArray("items")
                for (i in 0 until itemsArray.length()) {
                    val itemObj = itemsArray.getJSONObject(i)
                    val word = itemObj.getString("word")
                    val reading = itemObj.optString("reading", "")
                    val meaning = itemObj.getString("meaning")
                    val type = itemObj.optString("type", "vocab")
                    val notes = itemObj.optString("notes", "")
                    val exampleSentence = itemObj.optString("exampleSentence", "")
                    val isExposed = itemObj.optBoolean("isExposed", true)
                    
                    repository.insertItem(
                        VocabItem(
                            chapterId = chapterId.toInt(),
                            word = word,
                            reading = reading,
                            meaning = meaning,
                            type = type,
                            notes = notes,
                            exampleSentence = exampleSentence,
                            isExposed = isExposed
                        )
                    )
                }
                
                // Select the imported chapter automatically
                _selectedChapterId.value = chapterId.toInt()
                notifyWidgetUpdate()
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }
}
