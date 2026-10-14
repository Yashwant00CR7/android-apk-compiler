package com.yashwant.compiler

import java.io.File

enum class CompilationStage {
    INIT,
    RESOURCES_COMPILE,
    RESOURCES_LINK,
    JAVA_COMPILATION,
    DEXING,
    PACKAGING,
    SIGNING,
    COMPLETED,
    FAILED
}

data class CompilationProgress(
    val stage: CompilationStage,
    val message: String,
    val progressPercent: Int
)

data class CompilerError(
    val stage: CompilationStage,
    val file: String? = null,
    val line: Int? = null,
    val column: Int? = null,
    val message: String
)

data class CompilerResult(
    val success: Boolean,
    val apkFile: File? = null,
    val appName: String = "",
    val packageName: String = "",
    val versionCode: Int = 1,
    val versionName: String = "1.0",
    val sizeBytes: Long = 0L,
    val durationMs: Long = 0L,
    val failedStage: CompilationStage? = null,
    val errorMessage: String? = null,
    val errors: List<CompilerError> = emptyList(),
    val rawOutput: String = ""
)
