package com.example.data

import kotlinx.coroutines.flow.Flow

class AppRepository(private val appDao: AppDao) {
    val allChapters: Flow<List<Chapter>> = appDao.getAllChapters()
    val allItems: Flow<List<VocabItem>> = appDao.getAllItems()
    val exposedItemsCount: Flow<Int> = appDao.getExposedItemsCountFlow()

    fun getItemsForChapter(chapterId: Int): Flow<List<VocabItem>> = appDao.getItemsForChapter(chapterId)
    suspend fun getItemsForChapterList(chapterId: Int): List<VocabItem> = appDao.getItemsForChapterList(chapterId)

    suspend fun getChapterById(id: Int): Chapter? = appDao.getChapterById(id)
    suspend fun insertChapter(chapter: Chapter): Long = appDao.insertChapter(chapter)
    suspend fun updateChapter(chapter: Chapter) = appDao.updateChapter(chapter)
    suspend fun deleteChapter(chapter: Chapter) = appDao.deleteChapter(chapter)
    suspend fun deleteAllChapters() = appDao.deleteAllChapters()

    suspend fun getItemById(id: Int): VocabItem? = appDao.getItemById(id)
    suspend fun insertItem(item: VocabItem): Long = appDao.insertItem(item)
    suspend fun updateItem(item: VocabItem) = appDao.updateItem(item)
    suspend fun deleteItem(item: VocabItem) = appDao.deleteItem(item)

    suspend fun getExposedItems(): List<VocabItem> = appDao.getExposedItems()
}
