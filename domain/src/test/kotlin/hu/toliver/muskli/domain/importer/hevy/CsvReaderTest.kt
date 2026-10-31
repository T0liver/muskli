package hu.toliver.muskli.domain.importer.hevy

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class CsvReaderTest {
    private fun fields(text: String) = readCsv(text).map { it.fields }

    @Test
    fun `TC-HEVY-02 doubled quote inside quotes is one quote`() {
        assertEquals(listOf(listOf("Felt \"strong\"", "x")), fields("\"Felt \"\"strong\"\"\",x"))
    }

    @Test
    fun `TC-HEVY-02 comma inside quotes stays in the field`() {
        assertEquals(listOf(listOf("Push, heavy", "1")), fields("\"Push, heavy\",1"))
    }

    @Test
    fun `TC-HEVY-02 CRLF and LF inside quotes become LF`() {
        assertEquals(listOf(listOf("line one\nline two", "1")), fields("\"line one\r\nline two\",1"))
        assertEquals(listOf(listOf("line one\nline two", "1")), fields("\"line one\nline two\",1"))
    }

    @Test
    fun `TC-HEVY-02 CRLF and LF both end a record`() {
        assertEquals(
            listOf(listOf("a", "b"), listOf("c", "d"), listOf("e", "f")),
            fields("a,b\r\nc,d\ne,f"),
        )
    }

    @Test
    fun `TC-HEVY-02 final newline is optional`() {
        val expected = listOf(listOf("a"), listOf("b"))
        assertEquals(expected, fields("a\r\nb"))
        assertEquals(expected, fields("a\r\nb\r\n"))
        assertEquals(expected, fields("a\nb\n"))
    }

    @Test
    fun `TC-HEVY-02 UTF-8 BOM is ignored`() {
        assertEquals(listOf(listOf("title", "x")), fields("\uFEFF\"title\",x"))
    }

    @Test
    fun `TC-HEVY-02 empty fields are kept, quoted or bare`() {
        assertEquals(listOf(listOf("", "", "", "")), fields(",\"\",,"))
    }

    @Test
    fun `TC-HEVY-02 records are numbered from 1 and a multi-line field is one record`() {
        val records = readCsv("h1,h2\r\n\"x\r\ny\",1\r\nz,2")
        assertEquals(listOf(1, 2, 3), records.map { it.number })
        assertEquals(listOf("z", "2"), records[2].fields)
    }

    @Test
    fun `an empty text has no records`() {
        assertEquals(emptyList(), readCsv(""))
        assertEquals(emptyList(), readCsv("\uFEFF"))
    }
}
