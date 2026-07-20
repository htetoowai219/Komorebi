package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(tableName = "chapters")
data class Chapter(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val isExposed: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "vocab_items",
    foreignKeys = [
        ForeignKey(
            entity = Chapter::class,
            parentColumns = ["id"],
            childColumns = ["chapterId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["chapterId"])]
)
data class VocabItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val chapterId: Int,
    val word: String,             // e.g. "日本語", "犬", "食べる" or Kanji like "日"
    val reading: String,          // e.g. "にほんご", "いぬ", "たべる", "ひ"
    val meaning: String,          // e.g. "Japanese", "Dog", "to eat", "Day / Sun"
    val type: String,             // "vocab" or "kanji"
    val notes: String = "",        // any notes or sample sentences
    val exampleSentence: String = "", // dedicated field for example sentence
    val isExposed: Boolean = true, // individual exposure toggle
    val createdAt: Long = System.currentTimeMillis()
)
