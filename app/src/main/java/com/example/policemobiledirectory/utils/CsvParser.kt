package com.example.policemobiledirectory.utils

import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader

object CsvParser {
    fun parseCsvOrTsv(inputStream: InputStream): List<Map<String, String>> {
        val reader = BufferedReader(InputStreamReader(inputStream, "UTF-8"))
        val lines = reader.readLines()
        if (lines.isEmpty()) return emptyList()

        var headerLine = lines.first()
        if (headerLine.startsWith("\uFEFF")) {
            headerLine = headerLine.substring(1)
        }

        val delimiter = if (headerLine.contains("\t")) "\t" else ","
        val headers = splitLine(headerLine, delimiter)

        val records = mutableListOf<Map<String, String>>()
        for (i in 1 until lines.size) {
            val line = lines[i]
            if (line.trim().isEmpty()) continue
            val values = splitLine(line, delimiter)
            val record = mutableMapOf<String, String>()
            headers.forEachIndexed { index, header ->
                if (index < values.size) {
                    record[header.trim()] = values[index].trim()
                } else {
                    record[header.trim()] = ""
                }
            }
            if (record.values.any { it.isNotEmpty() }) {
                records.add(record)
            }
        }
        return records
    }

    fun splitLine(line: String, delimiter: String): List<String> {
        if (delimiter == "\t") {
            return line.split("\t")
        }
        val result = mutableListOf<String>()
        val current = java.lang.StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            if (c == '\"') {
                inQuotes = !inQuotes
            } else if (c == ',' && !inQuotes) {
                result.add(current.toString())
                current.setLength(0)
            } else {
                current.append(c)
            }
            i++
        }
        result.add(current.toString())
        return result.map { it.trim().removeSurrounding("\"") }
    }
}
