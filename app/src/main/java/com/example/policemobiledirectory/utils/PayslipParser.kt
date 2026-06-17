package com.example.policemobiledirectory.utils

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.tasks.await

object PayslipParser {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    // The Master List of Columns for consistent Google Sheets alignment
    val MASTER_COLUMNS = listOf(
        "MONTH", "KGID", "NAME", "BASIC", "DA", "HRA", "CCA", "MED", "RATION", "UNIFORM",
        "SPL KIT", "CONVEYANCE", "HARDSHIP", "SPL_ALLOWANCE", "GROSS_SALARY", "",
        "PT", "EGIS", "GPF", "KGID_DED", "LIC", "LIC_TAX", "WATER", "GPF_LOAN", "KGID_LOAN", "BANK_LOAN", "FA",
        "TOTAL_DEDUCTIONS", "",
        "AROGYA", "BENEVOLENT", "SPORTSFUND", "TOTAL_LOCAL_RECOVERIES", "NET_SALARY",
        "EL_BALANCE", "HPL_BALANCE", "REMARKS"
    )

    // Regex patterns for extracting fields
    private val PATTERNS = mapOf(
        "MONTH" to listOf(
            Regex("Payslip\\s+For\\s+The\\s+Month\\s+Of\\s*[-:]\\s*([A-Za-z]+\\s+\\d{4})", RegexOption.IGNORE_CASE),
            Regex("Month\\s*[-:]\\s*([A-Za-z]+\\s+\\d{4})", RegexOption.IGNORE_CASE)
        ),
        "KGID" to listOf(
            Regex("KGID\\s*No[^\\d]*[:\\s]+(\\d+)", RegexOption.IGNORE_CASE),
            Regex("KGID\\s*No[^\\d]*(\\d+)", RegexOption.IGNORE_CASE)
        ),
        "NAME" to listOf(
            Regex("Employee\\s*Name\\s*[:\\s]+(?:Mr\\.?|Mrs\\.?|Ms\\.?|Sri\\.?|Smt\\.?\\s*)?\\s*([A-Za-z\\s\\.\\-]+)", RegexOption.IGNORE_CASE)
        ),
        "BASIC" to listOf(Regex("Basic\\s*[:\\s]\\s*(\\d[\\d,]*)", RegexOption.IGNORE_CASE)),
        "DA" to listOf(Regex("\\bDA\\s*[:\\s]\\s*(\\d[\\d,]*)", RegexOption.IGNORE_CASE)),
        "HRA" to listOf(Regex("\\bHRA\\s*[:\\s]\\s*(\\d[\\d,]*)", RegexOption.IGNORE_CASE)),
        "CCA" to listOf(Regex("\\bCCA\\s*[:\\s]\\s*(\\d[\\d,]*)", RegexOption.IGNORE_CASE)),
        "UNIFORM" to listOf(
            Regex("UNIFM[_\\s]MNTS[_\\s]ALLW\\s*[:\\s]\\s*(\\d[\\d,]*)", RegexOption.IGNORE_CASE),
            Regex("UNIFORM\\s*[:\\s]\\s*(\\d[\\d,]*)", RegexOption.IGNORE_CASE)
        ),
        "MED" to listOf(Regex("\\bMED\\s*[:\\s]\\s*(\\d[\\d,]*)", RegexOption.IGNORE_CASE)),
        "RATION" to listOf(
            Regex("RATION\\s*ALLOW\\s*[:\\s]\\s*(\\d[\\d,]*)", RegexOption.IGNORE_CASE),
            Regex("\\bRATION\\s*[:\\s]\\s*(\\d[\\d,]*)", RegexOption.IGNORE_CASE)
        ),
        "CONVEYANCE" to listOf(
            Regex("CONVEYANCE(?:ALLOW)?\\s*[:\\s]\\s*(\\d[\\d,]*)", RegexOption.IGNORE_CASE),
            Regex("\\bCONVEY\\s*[:\\s]\\s*(\\d[\\d,]*)", RegexOption.IGNORE_CASE)
        ),
        "HARDSHIP" to listOf(
            Regex("HARDSHIP(?:ALLOW)?\\s*[:\\s]\\s*(\\d[\\d,]*)", RegexOption.IGNORE_CASE),
            Regex("\\bHARD\\s*[:\\s]\\s*(\\d[\\d,]*)", RegexOption.IGNORE_CASE)
        ),

        "SPL KIT" to listOf(
            Regex("SPL\\s*KIT\\s*[:\\s]\\s*(\\d[\\d,]*)", RegexOption.IGNORE_CASE),
            Regex("\\bKIT\\s*[:\\s]\\s*(\\d[\\d,]*)", RegexOption.IGNORE_CASE)
        ),
        "SPL_ALLOWANCE" to listOf(Regex("SPL\\s*ALLOW\\s*[:\\s]\\s*(\\d[\\d,]*)", RegexOption.IGNORE_CASE)),
        "GROSS_SALARY" to listOf(
            Regex("Gross\\s*Salary[^:]*[:\\s]+Rs\\.?\\s*(\\d[\\d,]*)", RegexOption.IGNORE_CASE),
            Regex("Gross\\s*[:\\s]+Rs\\.?\\s*(\\d[\\d,]*)", RegexOption.IGNORE_CASE),
            Regex("Rs\\.(\\d{5,})\\s*Sum", RegexOption.IGNORE_CASE)
        ),
        "PT" to listOf(Regex("\\bPT\\s*[:\\s]\\s*(\\d[\\d,]*)", RegexOption.IGNORE_CASE)),
        "EGIS" to listOf(
            Regex("(?:KGEGIS|KG-EGIS|EGIS|GIS|E\\.G\\.I\\.S|K\\.G\\.E\\.G\\.I\\.S|Employee Group Insurance Scheme|Employee Group Insurance|Group Insurance)\\s*[:\\s]\\s*(\\d[\\d,]*)", RegexOption.IGNORE_CASE),
            Regex("(?:KGEGIS|KG-EGIS|EGIS|GIS|E\\.G\\.I\\.S|K\\.G\\.E\\.G\\.I\\.S|Employee Group Insurance Scheme|Employee Group Insurance|Group Insurance)\\b[^\\d]*(\\d[\\d,]*)", RegexOption.IGNORE_CASE)
        ),
        "LIC" to listOf(Regex("\\bLIC\\s*[:\\s]\\s*(\\d[\\d,]*)", RegexOption.IGNORE_CASE)),
        "GPF" to listOf(
            Regex("GPF\\s*[:\\s]\\s*(\\d+)", RegexOption.IGNORE_CASE),
            Regex("\\bGPF\\b\\s+(\\d{3,6})\\b", RegexOption.IGNORE_CASE)
        ),
        "KGID_DED" to listOf(
            Regex("KGID\\s*[:\\s]\\s*(\\d+)", RegexOption.IGNORE_CASE),
            Regex("\\bKGID\\b\\s+(\\d{3,6})\\b", RegexOption.IGNORE_CASE)
        ),
        "LIC_TAX" to listOf(
            Regex("LICTX\\s*[:\\s]\\s*(\\d[\\d,]*)", RegexOption.IGNORE_CASE),
            Regex("LIC\\s*TAX\\s*[:\\s]\\s*(\\d[\\d,]*)", RegexOption.IGNORE_CASE)
        ),
        "WATER" to listOf(
            Regex("WATER\\s*(?:CHARGES|DED)?\\s*[:\\s]\\s*(\\d[\\d,]*)", RegexOption.IGNORE_CASE),
            Regex("\\bWATER\\b[^\\d]*(\\d[\\d,]*)", RegexOption.IGNORE_CASE)
        ),
        "KGID_LOAN" to listOf(Regex("KGID[_\\s]*LOAN\\s*[:\\s]\\s*(\\d[\\d,]*)", RegexOption.IGNORE_CASE)),
        "GPF_LOAN" to listOf(
            Regex("GPF[_\\s]*LOAN\\s*[:\\s]\\s*(\\d[\\d,]*)", RegexOption.IGNORE_CASE),
            Regex("GP[_\\s]*LOAN\\s*[:\\s]\\s*(\\d[\\d,]*)", RegexOption.IGNORE_CASE)
        ),
        "BANK_LOAN" to listOf(
            Regex("B\\.LOAN\\s*[:\\s]\\s*(\\d[\\d,]*)", RegexOption.IGNORE_CASE),
            Regex("BANK\\s*LOAN\\s*[:\\s]\\s*(\\d[\\d,]*)", RegexOption.IGNORE_CASE)
        ),
        "FA" to listOf(Regex("\\bFA\\s*[:\\s]\\s*(\\d[\\d,]*)", RegexOption.IGNORE_CASE)),
        "TOTAL_DEDUCTIONS" to listOf(
            Regex("Sum\\s*of\\s*Deductions(?:\\s*&\\s*Recoveries)?[^\\d]+(\\d[\\d,]*)", RegexOption.IGNORE_CASE),
            Regex("Total\\s*Deductions[^\\d]+(\\d[\\d,]*)", RegexOption.IGNORE_CASE),
            Regex("Sum\\s*of\\s*Deductions[\\s\\S]*?Rs\\.?\\s*(\\d[\\d,]*)", RegexOption.IGNORE_CASE),
            Regex("Deductions[\\s\\S]{0,15}?(\\d[\\d,]+)", RegexOption.IGNORE_CASE)
        ),
        "AROGYA" to listOf(
            Regex("AROGYA\\s*[:\\s]\\s*(\\d[\\d,]*)", RegexOption.IGNORE_CASE),
            Regex("\\bABY\\s*[:\\s]\\s*(\\d[\\d,]*)", RegexOption.IGNORE_CASE),
            Regex("\\bABY\\b[^\\d]*(\\d[\\d,]*)", RegexOption.IGNORE_CASE)
        ),
        "BENEVOLENT" to listOf(
            Regex("BENEVOLENT\\s*[:\\s]\\s*(\\d[\\d,]*)", RegexOption.IGNORE_CASE),
            Regex("KSPBF\\s*[:\\s]\\s*(\\d[\\d,]*)", RegexOption.IGNORE_CASE)
        ),
        "SPORTSFUND" to listOf(
            Regex("SPORTSFUND\\s*[:\\s]\\s*(\\d[\\d,]*)", RegexOption.IGNORE_CASE),
            Regex("\\bSPORTS\\s*[:\\s]\\s*(\\d[\\d,]*)", RegexOption.IGNORE_CASE)
        ),
        "TOTAL_LOCAL_RECOVERIES" to listOf(
            Regex("Total\\s*Local\\s*Recov[^\\d]+(\\d[\\d,]*)", RegexOption.IGNORE_CASE),
            Regex("Total\\s*Local\\s*Recov.*?Rs\\.?\\s*(\\d[\\d,]*)", RegexOption.IGNORE_CASE)
        ),
        "NET_SALARY" to listOf(
            Regex("Net\\s*Salary\\s*Payable\\s*[:\\s]+Rs\\.?\\s*(\\d[\\d,]*)", RegexOption.IGNORE_CASE),
            Regex("Net\\s*Salary[^P][^:]*[:\\s]+Rs\\.?\\s*(\\d[\\d,]*)", RegexOption.IGNORE_CASE),
            Regex("Net\\s*Salary\\s*[:\\s]+Rs\\.?\\s*(\\d[\\d,]*)", RegexOption.IGNORE_CASE)
        ),
        "EL_BALANCE" to listOf(
            Regex("EL\\s*Balance\\s*[:\\s]\\s*(\\d[\\d,]*)", RegexOption.IGNORE_CASE),
            Regex("\\bEL\\s*[:\\s]\\s*(\\d[\\d,]*)", RegexOption.IGNORE_CASE)
        ),
        "HPL_BALANCE" to listOf(
            Regex("HPL\\s*Balance\\s*[:\\s]\\s*(\\d[\\d,]*)", RegexOption.IGNORE_CASE),
            Regex("\\bHPL\\s*[:\\s]\\s*(\\d[\\d,]*)", RegexOption.IGNORE_CASE)
        )
    )

    suspend fun parsePayslip(context: Context, uri: Uri): ParseResult {
        return try {
            val image = InputImage.fromFilePath(context, uri)
            val result = recognizer.process(image).await()
            val text = reconstructTextByLines(result)

            Log.d("PayslipParser", "OCR Extracted Tabular Text:\n$text")

            val extractedData = mutableMapOf<String, String>()

            for (col in MASTER_COLUMNS) {
                if (col.isBlank()) continue
                
                val patterns = PATTERNS[col]
                var found = ""
                
                if (patterns != null) {
                    for (pattern in patterns) {
                        val match = pattern.find(text)
                        if (match != null) {
                            found = match.groupValues[1].replace(",", "").trim()
                            if (col == "NAME") {
                                found = found.replace(Regex("\\bDDO\\b", RegexOption.IGNORE_CASE), "").trim()
                                // Also remove if OCR missed the space (e.g. RAVIKUMARJDDO)
                                if (found.endsWith("DDO", ignoreCase = true)) {
                                    found = found.dropLast(3).trim()
                                }
                            }
                            break
                        }
                    }
                }
                
                extractedData[col] = found
            }

            ParseResult.Success(extractedData, text)
        } catch (e: Exception) {
            Log.e("PayslipParser", "Error parsing payslip", e)
            ParseResult.Error(e.message ?: "Failed to parse image")
        }
    }

    sealed class ParseResult {
        data class Success(val data: Map<String, String>, val rawText: String) : ParseResult()
        data class Error(val message: String) : ParseResult()
    }

    private fun reconstructTextByLines(result: com.google.mlkit.vision.text.Text): String {
        val lines = mutableListOf<com.google.mlkit.vision.text.Text.Line>()
        for (block in result.textBlocks) {
            lines.addAll(block.lines)
        }

        // Sort vertically by center Y
        lines.sortBy { it.boundingBox?.exactCenterY() ?: 0f }

        val rows = mutableListOf<MutableList<com.google.mlkit.vision.text.Text.Line>>()
        for (line in lines) {
            val y = line.boundingBox?.exactCenterY() ?: 0f
            val h = line.boundingBox?.height() ?: 0
            val threshold = (h / 2).coerceAtLeast(10).toFloat()
            
            var added = false
            if (rows.isNotEmpty()) {
                val lastRow = rows.last()
                val rowY = lastRow.map { it.boundingBox?.exactCenterY() ?: 0f }.average().toFloat()
                if (Math.abs(y - rowY) < threshold) {
                    lastRow.add(line)
                    added = true
                }
            }
            if (!added) {
                rows.add(mutableListOf(line))
            }
        }

        val stringBuilder = java.lang.StringBuilder()
        for (row in rows) {
            // Sort horizontally by center X
            row.sortBy { it.boundingBox?.exactCenterX() ?: 0f }
            val rowText = row.joinToString("   ") { it.text }
            stringBuilder.append(rowText).append("\n")
        }

        return stringBuilder.toString()
    }
}
