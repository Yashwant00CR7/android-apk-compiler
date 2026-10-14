package com.yashwant.compiler

object CompilerErrorParser {

    private val ECJ_ERROR_REGEX = Regex("""(?:\d+\.\s+ERROR\s+in\s+([^\s(]+)\s+\(at\s+line\s+(\d+)\)|([^:\n]+):(\d+):\s+(?:error|ERROR):\s*(.*))""")
    private val AAPT_ERROR_REGEX = Regex("""([^:\n]+):(\d+)(?::(\d+))?:\s+(?:error|ERROR):\s*(.*)""")

    fun parseErrors(stage: CompilationStage, rawOutput: String): List<CompilerError> {
        val errors = mutableListOf<CompilerError>()
        if (rawOutput.isBlank()) return errors

        val lines = rawOutput.lines()
        var currentFile: String? = null
        var currentLine: Int? = null
        val msgBuilder = StringBuilder()

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) continue

            // 1. Try ECJ pattern
            val ecjMatch = ECJ_ERROR_REGEX.find(trimmed)
            if (ecjMatch != null) {
                if (currentFile != null && msgBuilder.isNotEmpty()) {
                    errors.add(CompilerError(stage, currentFile, currentLine, null, msgBuilder.toString().trim()))
                    msgBuilder.clear()
                }
                currentFile = ecjMatch.groupValues[1].takeIf { it.isNotEmpty() }
                    ?: ecjMatch.groupValues[3].takeIf { it.isNotEmpty() }
                currentLine = (ecjMatch.groupValues[2].takeIf { it.isNotEmpty() }
                    ?: ecjMatch.groupValues[4].takeIf { it.isNotEmpty() })?.toIntOrNull()
                val inlineMsg = ecjMatch.groupValues.getOrNull(5)
                if (!inlineMsg.isNullOrBlank()) {
                    msgBuilder.append(inlineMsg)
                }
                continue
            }

            // 2. Try AAPT pattern
            val aaptMatch = AAPT_ERROR_REGEX.find(trimmed)
            if (aaptMatch != null) {
                if (currentFile != null && msgBuilder.isNotEmpty()) {
                    errors.add(CompilerError(stage, currentFile, currentLine, null, msgBuilder.toString().trim()))
                    msgBuilder.clear()
                }
                currentFile = aaptMatch.groupValues[1]
                currentLine = aaptMatch.groupValues[2].toIntOrNull()
                val col = aaptMatch.groupValues.getOrNull(3)?.toIntOrNull()
                val msg = aaptMatch.groupValues[4]
                errors.add(CompilerError(stage, currentFile, currentLine, col, msg.trim()))
                currentFile = null
                currentLine = null
                continue
            }

            if (currentFile != null && !trimmed.startsWith("----------") && !trimmed.contains("problem (")) {
                if (msgBuilder.isNotEmpty()) msgBuilder.append(" ")
                msgBuilder.append(trimmed)
            }
        }

        if (currentFile != null && msgBuilder.isNotEmpty()) {
            errors.add(CompilerError(stage, currentFile, currentLine, null, msgBuilder.toString().trim()))
        }

        if (errors.isEmpty() && rawOutput.isNotBlank()) {
            errors.add(CompilerError(stage, message = rawOutput.lines().firstOrNull { it.contains("error", ignoreCase = true) } ?: rawOutput.take(200)))
        }

        return errors
    }
}
