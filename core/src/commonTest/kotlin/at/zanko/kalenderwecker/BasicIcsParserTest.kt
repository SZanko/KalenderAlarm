package at.zanko.kalenderwecker

import at.zanko.kalenderwecker.ics.parse
import kotlin.test.Test
import kotlin.test.assertTrue
import com.goncalossilva.resources.Resource
import kotlin.test.assertEquals

class BasicIcsParserTest {
    @Test
    fun `example data exists`() {
        val resource = Resource("TimeEdit_Short.ics")
        assertTrue(resource.exists())
    }

    @Test
    fun gIVEN_validIcsFileAsString_WHEN_parse_THEN_returnEventList() {
        val ics = Resource("TimeEdit_UltraShort.ics")
        assertTrue(ics.exists())

        val result = parse(ics.readText())
        assertEquals(3, result.size)
    }
}