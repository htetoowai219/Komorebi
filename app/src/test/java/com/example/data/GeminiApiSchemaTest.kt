package com.example.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class GeminiApiSchemaTest {

    @Test
    fun schemaIsAStrictVocabularyObject() {
        val root = GeminiApi.buildResponseSchema()

        assertEquals("OBJECT", root.getString("type"))
        val properties = root.getJSONObject("properties")
        assertEquals("STRING", properties.getJSONObject("title").getString("type"))

        val items = properties.getJSONObject("items")
        assertEquals("ARRAY", items.getString("type"))
        val item = items.getJSONObject("items")
        assertEquals("OBJECT", item.getString("type"))
        val itemProperties = item.getJSONObject("properties")
        for (field in listOf("word", "reading", "meaning", "type", "notes", "example")) {
            assertEquals("STRING", itemProperties.getJSONObject(field).getString("type"))
        }

        val required = root.getJSONArray("required")
            .let { arr -> (0 until arr.length()).map { arr.getString(it) } }
        assertTrue("title" in required)
        assertTrue("items" in required)
    }

    @Test
    fun schemaConstrainedOutputParsesAsExtraction() {
        val schemaShapedOutput = """
            {"title":"Genki Ch 3","items":[
              {"word":"映画","reading":"えいが","meaning":"movie","type":"vocab","notes":"N4","example":"映画を見ます。"}
            ]}
        """.trimIndent()

        val result = GeminiParser.parse(schemaShapedOutput)

        assertNotNull(result)
        result!!
        assertEquals("Genki Ch 3", result.title)
        assertEquals("映画", result.items[0].word)
        assertEquals("vocab", result.items[0].type)
    }
}