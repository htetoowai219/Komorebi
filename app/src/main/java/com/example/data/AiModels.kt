package com.example.data

/**
 * A single vocabulary entry extracted by the AI, held in an editable state
 * before the user confirms the import.
 */
data class AiVocabDraft(
    val word: String = "",
    val reading: String = "",
    val meaning: String = "",
    val type: String = "vocab",
    val notes: String = "",
    val example: String = "",
    val selected: Boolean = true
)

/** The full result of an AI extraction: a suggested chapter plus its items. */
data class AiExtraction(
    val title: String = "",
    val items: List<AiVocabDraft> = emptyList()
)

/** UI state for the photo import flow. */
sealed interface AiImportUiState {
    data object Idle : AiImportUiState
    data object Loading : AiImportUiState
    data class Error(val message: String) : AiImportUiState
    data class Success(val extraction: AiExtraction) : AiImportUiState
}