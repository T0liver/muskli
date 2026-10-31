package hu.toliver.muskli.domain.importer.hevy

/** One CSV record; [number] is 1-based and counts records, not lines (a quoted field may span lines). */
internal data class CsvRecord(val number: Int, val fields: List<String>)

/**
 * RFC 4180 reader (D-040). Accepts CRLF, LF, or a lone CR as a record end, an optional UTF-8 BOM, and a
 * missing final newline. Line breaks inside quoted fields become `\n`, so the result does not depend on
 * how git checked the file out. Malformed quoting is read leniently (an unclosed quote runs to the end,
 * text after a closing quote is kept), because the field-count check then reports the record.
 */
internal fun readCsv(text: String): List<CsvRecord> {
    val records = mutableListOf<CsvRecord>()
    val fields = mutableListOf<String>()
    val field = StringBuilder()
    var inQuotes = false
    var atFieldStart = true
    var recordStarted = false

    fun endField() {
        fields += field.toString()
        field.clear()
        atFieldStart = true
    }

    fun endRecord() {
        endField()
        records += CsvRecord(records.size + 1, fields.toList())
        fields.clear()
        recordStarted = false
    }

    var i = if (text.startsWith('\uFEFF')) 1 else 0
    while (i < text.length) {
        val c = text[i]
        val isLineBreak = c == '\n' || c == '\r'
        if (c == '\r' && text.getOrNull(i + 1) == '\n') i++
        recordStarted = true
        when {
            inQuotes && c == '"' && text.getOrNull(i + 1) == '"' -> {
                field.append('"')
                i++
            }
            inQuotes && c == '"' -> inQuotes = false
            inQuotes && isLineBreak -> field.append('\n')
            inQuotes -> field.append(c)
            c == '"' && atFieldStart -> {
                inQuotes = true
                atFieldStart = false
            }
            c == ',' -> endField()
            isLineBreak -> endRecord()
            else -> {
                field.append(c)
                atFieldStart = false
            }
        }
        i++
    }
    if (recordStarted) endRecord()
    return records
}
