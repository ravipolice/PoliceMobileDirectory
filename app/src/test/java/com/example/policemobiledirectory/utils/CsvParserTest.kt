package com.example.policemobiledirectory.utils

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.ByteArrayInputStream

class CsvParserTest {

    @Test
    fun `splitLine handles standard CSV commas`() {
        val line = "value1,value2,value3"
        val parts = CsvParser.splitLine(line, ",")
        assertEquals(listOf("value1", "value2", "value3"), parts)
    }

    @Test
    fun `splitLine handles quoted commas`() {
        val line = "\"value1, with comma\",value2,\"value3\""
        val parts = CsvParser.splitLine(line, ",")
        assertEquals(listOf("value1, with comma", "value2", "value3"), parts)
    }

    @Test
    fun `splitLine handles tabs`() {
        val line = "value1\tvalue2\tvalue3"
        val parts = CsvParser.splitLine(line, "\t")
        assertEquals(listOf("value1", "value2", "value3"), parts)
    }

    @Test
    fun `parseCsvOrTsv parses standard CSV stream`() {
        val csvData = """
            kgid,name,email
            123,John Doe,john@example.com
            456,Jane Smith,jane@example.com
        """.trimIndent()

        val inputStream = ByteArrayInputStream(csvData.toByteArray(Charsets.UTF_8))
        val records = CsvParser.parseCsvOrTsv(inputStream)

        assertEquals(2, records.size)
        assertEquals("123", records[0]["kgid"])
        assertEquals("John Doe", records[0]["name"])
        assertEquals("john@example.com", records[0]["email"])

        assertEquals("456", records[1]["kgid"])
        assertEquals("Jane Smith", records[1]["name"])
        assertEquals("jane@example.com", records[1]["email"])
    }

    @Test
    fun `parseCsvOrTsv parses TSV with BOM`() {
        val tsvData = "\uFEFFkgid\tname\temail\n789\tAlice\talice@example.com"
        val inputStream = ByteArrayInputStream(tsvData.toByteArray(Charsets.UTF_8))
        val records = CsvParser.parseCsvOrTsv(inputStream)

        assertEquals(1, records.size)
        assertEquals("789", records[0]["kgid"])
        assertEquals("Alice", records[0]["name"])
        assertEquals("alice@example.com", records[0]["email"])
    }
}
