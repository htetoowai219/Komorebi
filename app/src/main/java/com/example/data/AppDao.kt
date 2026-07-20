package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    // Chapters
    @Query("SELECT * FROM chapters ORDER BY name ASC")
    fun getAllChapters(): Flow<List<Chapter>>

    @Query("SELECT * FROM chapters WHERE id = :id")
    suspend fun getChapterById(id: Int): Chapter?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChapter(chapter: Chapter): Long

    @Update
    suspend fun updateChapter(chapter: Chapter)

    @Delete
    suspend fun deleteChapter(chapter: Chapter)

    // Vocab / Kanji Items
    @Query("SELECT * FROM vocab_items WHERE chapterId = :chapterId ORDER BY createdAt DESC")
    fun getItemsForChapter(chapterId: Int): Flow<List<VocabItem>>

    @Query("SELECT * FROM vocab_items ORDER BY createdAt DESC")
    fun getAllItems(): Flow<List<VocabItem>>

    @Query("SELECT * FROM vocab_items WHERE id = :id")
    suspend fun getItemById(id: Int): VocabItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: VocabItem): Long

    @Update
    suspend fun updateItem(item: VocabItem)

    @Delete
    suspend fun deleteItem(item: VocabItem)

    // Helper for Widget query - gets all exposed items where their chapter is also exposed
    @Query("""
        SELECT v.* FROM vocab_items v 
        INNER JOIN chapters c ON v.chapterId = c.id 
        WHERE v.isExposed = 1 AND c.isExposed = 1
    """)
    suspend fun getExposedItems(): List<VocabItem>

    // Helper to count how many items are currently active/exposed
    @Query("""
        SELECT COUNT(v.id) FROM vocab_items v 
        INNER JOIN chapters c ON v.chapterId = c.id 
        WHERE v.isExposed = 1 AND c.isExposed = 1
    """)
    fun getExposedItemsCountFlow(): Flow<Int>
}
