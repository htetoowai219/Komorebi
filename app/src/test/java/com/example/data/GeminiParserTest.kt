package com.example.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class GeminiParserTest {

    @Test
    fun parsesCleanJson() {
        val json = """
            {"title":"Genki Ch 3","items":[
              {"word":"映画","reading":"えいが","meaning":"movie","type":"vocab","notes":"N4","example":"映画を見ます。"},
              {"word":"犬","reading":"いぬ","meaning":"dog","type":"vocab","notes":"","example":""}
            ]}
        """.trimIndent()

        val result = GeminiParser.parse(json)

        assertNotNull(result)
        result!!
        assertEquals("Genki Ch 3", result.title)
        assertEquals(2, result.items.size)
        assertEquals("映画", result.items[0].word)
        assertEquals("えいが", result.items[0].reading)
        assertEquals("movie", result.items[0].meaning)
        assertEquals("vocab", result.items[0].type)
        assertEquals("N4", result.items[0].notes)
        assertEquals("映画を見ます。", result.items[0].example)
        assertTrue(result.items[0].selected)
    }

    @Test
    fun toleratesMarkdownFences() {
        val json = "```json\n{\"title\":\"Kanji\",\"items\":[{\"word\":\"日\",\"reading\":\"ひ\",\"meaning\":\"day\",\"type\":\"kanji\"}]}\n```"

        val result = GeminiParser.parse(json)

        assertNotNull(result)
        result!!
        assertEquals("Kanji", result.title)
        assertEquals(1, result.items.size)
        assertEquals("kanji", result.items[0].type)
    }

    @Test
    fun ignoresSurroundingProse() {
        val json = "Here you go:\n{\"title\":\"Words\",\"items\":[{\"word\":\"本\",\"reading\":\"ほん\",\"meaning\":\"book\"}]}\nHope that helps!"

        val result = GeminiParser.parse(json)

        assertNotNull(result)
        result!!
        assertEquals("本", result.items[0].word)
    }

    @Test
    fun defaultsMissingFieldsSafely() {
        val json = """{"title":"","items":[{"word":"一"}]}"""

        val result = GeminiParser.parse(json)

        assertNotNull(result)
        result!!
        assertEquals("", result.title)
        assertEquals(1, result.items.size)
        assertEquals("", result.items[0].reading)
        assertEquals("", result.items[0].meaning)
        assertEquals("vocab", result.items[0].type)
    }

    @Test
    fun skipsEntriesWithoutWord() {
        val json = """{"title":"T","items":[{"word":"猫","reading":"ねこ"},{"reading":"foo"}]}"""

        val result = GeminiParser.parse(json)

        assertNotNull(result)
        result!!
        assertEquals(1, result.items.size)
        assertEquals("猫", result.items[0].word)
    }

    @Test
    fun returnsEmptyExtractionForEmptyItems() {
        val json = """{"title":"Empty","items":[]}"""

        val result = GeminiParser.parse(json)

        assertNotNull(result)
        result!!
        assertTrue(result.items.isEmpty())
    }

    @Test
    fun returnsNullForNonJson() {
        assertNull(GeminiParser.parse("Sorry, I could not read the image."))
        assertNull(GeminiParser.parse(""))
    }

    @Test
    fun returnsNullForBrokenJson() {
        assertNull(GeminiParser.parse("{\"title\":\"Broken\",\"items\":["))
    }
}