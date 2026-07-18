package com.example.util

import android.content.Context
import android.net.Uri
import com.example.data.model.Option
import com.example.data.model.Question
import java.io.BufferedReader
import java.io.InputStreamReader

object CSVHelper {

    fun exportToCSV(questions: List<Question>): String {
        if (questions.isEmpty()) return ""

        var maxOptions = 4
        questions.forEach { q ->
            if (q.options.size > maxOptions) {
                maxOptions = q.options.size
            }
        }

        val headers = mutableListOf<String>()
        headers.add("Question")
        for (i in 1..maxOptions) {
            headers.add("Option $i")
        }
        headers.add("Correct Answer (Comma separated if multiple)")
        headers.add("Explanation")

        val sb = StringBuilder()
        sb.append(headers.joinToString(",") { escapeCSV(it) }).append("\n")

        questions.forEach { q ->
            val row = mutableListOf<String>()
            row.add(q.questionText)
            for (i in 0 until maxOptions) {
                val optText = q.options.getOrNull(i)?.text ?: ""
                row.add(optText)
            }
            
            val correctStr = if (q.isNoAnswer) {
                "0"
            } else {
                q.correctIndices.map { it + 1 }.joinToString(",")
            }
            row.add(correctStr)
            row.add(q.explanation ?: "")

            sb.append(row.joinToString(",") { escapeCSV(it) }).append("\n")
        }

        return sb.toString()
    }

    private fun escapeCSV(field: String): String {
        if (field.contains("\"") || field.contains(",") || field.contains("\n") || field.contains("\r")) {
            return "\"" + field.replace("\"", "\"\"") + "\""
        }
        return field
    }

    fun parseCSV(context: Context, uri: Uri): List<Question> {
        val questions = mutableListOf<Question>()
        try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return emptyList()
            val reader = BufferedReader(InputStreamReader(inputStream, "UTF-8"))
            val lines = mutableListOf<String>()
            var line: String? = reader.readLine()
            while (line != null) {
                lines.add(line)
                line = reader.readLine()
            }
            
            val parsedLines = parseCSVLines(lines.joinToString("\n"))
            if (parsedLines.size < 2) return emptyList() // No data rows
            
            // First row is header
            for (i in 1 until parsedLines.size) {
                val row = parsedLines[i]
                if (row.size < 4) continue
                
                val questionText = row.getOrNull(0)?.trim() ?: ""
                if (questionText.isEmpty()) continue
                
                val explanationText = row.lastOrNull()?.trim() ?: ""
                val correctField = row.getOrNull(row.size - 2)?.trim() ?: ""
                
                var isNoAnswer = correctField == "0" || correctField.lowercase() == "none" || correctField.isEmpty()
                var correctIndices = mutableListOf<Int>()
                
                if (!isNoAnswer) {
                    correctIndices = correctField.split(",")
                        .mapNotNull { it.trim().toIntOrNull()?.minus(1) }
                        .filter { it >= 0 }
                        .toMutableList()
                    if (correctIndices.isEmpty()) {
                        isNoAnswer = true
                    }
                }
                
                val options = mutableListOf<Option>()
                // Options are in between first field and second to last field
                for (j in 1 until row.size - 2) {
                    val optText = row.getOrNull(j)?.trim() ?: ""
                    if (optText.isNotEmpty() || j <= 2) {
                        options.add(Option(text = optText))
                    }
                }
                
                while (options.size < 2) {
                    options.add(Option(text = ""))
                }
                
                correctIndices = correctIndices.filter { it < options.size }.toMutableList()
                
                questions.add(
                    Question(
                        questionText = questionText,
                        options = options,
                        correctIndices = correctIndices,
                        isNoAnswer = isNoAnswer,
                        explanation = explanationText,
                        qImgSize = 160,
                        optImgSize = 160,
                        expImgSize = 160
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return questions
    }

    private fun parseCSVLines(text: String): List<List<String>> {
        val result = mutableListOf<List<String>>()
        var currentField = StringBuilder()
        var currentRow = mutableListOf<String>()
        var inQuotes = false
        var i = 0
        while (i < text.length) {
            val c = text[i]
            val nextC = text.getOrNull(i + 1)
            
            if (c == '"') {
                if (inQuotes && nextC == '"') {
                    currentField.append('"')
                    i += 2
                    continue
                } else {
                    inQuotes = !inQuotes
                    i++
                    continue
                }
            }
            
            if (c == ',' && !inQuotes) {
                currentRow.add(currentField.toString())
                currentField = StringBuilder()
                i++
                continue
            }
            
            if ((c == '\n' || c == '\r') && !inQuotes) {
                currentRow.add(currentField.toString())
                currentField = StringBuilder()
                if (currentRow.isNotEmpty()) {
                    result.add(currentRow)
                    currentRow = mutableListOf()
                }
                if (c == '\r' && nextC == '\n') {
                    i += 2
                } else {
                    i++
                }
                continue
            }
            
            currentField.append(c)
            i++
        }
        if (currentRow.isNotEmpty() || currentField.isNotEmpty()) {
            currentRow.add(currentField.toString())
            result.add(currentRow)
        }
        return result
    }
}
