package com.example.data

import android.util.Base64
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * Minimal client for the Gemini Developer API. Sends a photo of a vocab list
 * and asks the model to transcribe it into structured JSON. Uses only
 * [HttpURLConnection] and [JSONObject] so no extra dependencies are needed.
 */
object GeminiApi {

    const val MODEL = "gemini-2.5-flash"
    private const val ENDPOINT =
        "https://generativelanguage.googleapis.com/v1beta/models/$MODEL:generateContent"
    private const val TIMEOUT_MS = 90_000
    private const val TAG = "GeminiApi"

    private val PROMPT = """
        You are importing a photo of a Japanese vocabulary list into a flashcard app.
        Read carefully EVERY word entry visible in the image and return it as JSON.

        For each entry extract:
        - "word": the Japanese term exactly as written (a kanji character, word, or phrase)
        - "reading": the kana (hiragana or katakana) reading, e.g. にほんご
        - "meaning": a concise English translation
        - "type": "kanji" only when the entry is a single standalone kanji character;
          otherwise "vocab"
        - "notes": any extra text printed next to the entry (e.g. an N-level tag or a
          grammar note), or "" if none
        - "example": a short natural Japanese example sentence using the word
          (invent one if the image has none), or "" only if you cannot

        Also suggest "title": a short chapter name that summarizes the list
        (e.g. "Genki Ch 3 Vocabulary").

        Respond with ONLY a single JSON object - no markdown fences, no commentary.
        Use exactly this shape:
        {"title":"...","items":[{"word":"...","reading":"...","meaning":"...","type":"vocab","notes":"...","example":"..."}]}
    """.trimIndent()

    /**
     * Sends the image to Gemini and returns the parsed extraction.
     * @throws IOException if the request fails or the model returns no text.
     * @throws IllegalStateException if the response is not valid vocabulary JSON.
     */
    suspend fun extractVocabulary(imageBytes: ByteArray, mimeType: String, apiKey: String): AiExtraction {
        val rawText = withContext(Dispatchers.IO) {
            postGenerateContent(imageBytes, mimeType, apiKey)
        }
        return GeminiParser.parse(rawText)
            ?: throw IllegalStateException(
                "The AI response was not valid vocabulary JSON. Please try again."
            )
    }

    private fun postGenerateContent(
        imageBytes: ByteArray,
        mimeType: String,
        apiKey: String
    ): String {
        val requestBody = JSONObject()
            .put(
                "contents", JSONArray().put(
                    JSONObject().put(
                        "parts", JSONArray()
                            .put(
                                JSONObject().put(
                                    "inline_data", JSONObject()
                                        .put("mime_type", mimeType)
                                        .put(
                                            "data",
                                            Base64.encodeToString(imageBytes, Base64.NO_WRAP)
                                        )
                                )
                            )
                            .put(JSONObject().put("text", PROMPT))
                    )
                )
            )
            .put(
                "generationConfig", JSONObject()
                    .put("temperature", 0.1)
                    .put("maxOutputTokens", 8192)
            )

        val url = URL("$ENDPOINT?key=$apiKey")
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.doOutput = true
        connection.connectTimeout = TIMEOUT_MS
        connection.readTimeout = TIMEOUT_MS
        connection.setRequestProperty("Content-Type", "application/json")
        connection.setRequestProperty("Accept", "application/json")
        try {
            connection.outputStream.use { it.write(requestBody.toString().toByteArray(Charsets.UTF_8)) }
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val responseText = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: ""
            if (code !in 200..299) {
                throw IOException("Gemini API error ($code): ${extractError(responseText)}")
            }
            val root = JSONObject(responseText)
            val text = root.optJSONArray("candidates")
                ?.optJSONObject(0)
                ?.optJSONObject("content")
                ?.optJSONArray("parts")
                ?.optJSONObject(0)
                ?.optString("text", "")
                ?: ""
            if (text.isBlank()) {
                throw IOException("The AI returned an empty response.")
            }
            return text
        } finally {
            connection.disconnect()
        }
    }

    private fun extractError(body: String): String {
        return try {
            JSONObject(body).optJSONObject("error")?.optString("message")
                ?: body.take(200)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse error body", e)
            body.take(200)
        }
    }
}

/** Pure parser that turns Gemini's text answer into an [AiExtraction]. */
object GeminiParser {

    /**
     * Parses the model output. Tolerates optional markdown fences and stray
     * prose around the JSON payload. Returns null when no usable JSON is found.
     */
    fun parse(raw: String): AiExtraction? {
        val body = extractJsonBody(raw) ?: return null
        return try {
            val root = JSONObject(body)
            val title = root.optString("title", "").trim()
            val rawItems = root.optJSONArray("items") ?: JSONArray()
            val items = mutableListOf<AiVocabDraft>()
            for (i in 0 until rawItems.length()) {
                val obj = rawItems.optJSONObject(i) ?: continue
                val word = obj.optString("word", "").trim()
                if (word.isEmpty()) continue
                items += AiVocabDraft(
                    word = word,
                    reading = obj.optString("reading", "").trim(),
                    meaning = obj.optString("meaning", "").trim(),
                    type = if (obj.optString("type", "") == "kanji") "kanji" else "vocab",
                    notes = obj.optString("notes", "").trim(),
                    example = obj.optString("example", "").trim()
                )
            }
            AiExtraction(title = title, items = items)
        } catch (e: Exception) {
            null
        }
    }

    private fun extractJsonBody(raw: String): String? {
        val noFences = FENCE_REGEX.replace(raw.trim(), "")
        val start = noFences.indexOf('{')
        if (start == -1) return null
        val end = noFences.lastIndexOf('}')
        if (end <= start) return null
        return noFences.substring(start, end + 1)
    }

    private val FENCE_REGEX = Regex(""""```[A-Za-z]*\s*""")
}