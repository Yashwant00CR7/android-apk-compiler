package com.yashwant.compiler

import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

class CompilerBootstrapManager(
    val baseDir: File,
    private val nativeLibDir: File? = null
) {
    val compilerDir: File = File(baseDir, "compiler")
    val androidJarFile: File = File(compilerDir, "android.jar")
    val debugKeystoreFile: File = File(compilerDir, "debug.keystore")

    fun initialize(): Boolean {
        return compilerDir.mkdirs() || compilerDir.exists()
    }

    fun extractCompilerAssets(
        assetOpener: (String) -> InputStream?
    ): Int {
        initialize()
        var extracted = 0

        // 1. Extract android.jar
        if (!androidJarFile.exists() || androidJarFile.length() == 0L) {
            assetOpener("compiler/android.jar")?.use { input ->
                FileOutputStream(androidJarFile).use { output ->
                    input.copyTo(output)
                }
                extracted++
            }
        }

        // 2. Extract debug.keystore
        if (!debugKeystoreFile.exists() || debugKeystoreFile.length() == 0L) {
            assetOpener("compiler/debug.keystore")?.use { input ->
                FileOutputStream(debugKeystoreFile).use { output ->
                    input.copyTo(output)
                }
                extracted++
            }
        }

        return extracted
    }

    fun getAapt2Executable(): File? {
        // Priority 1: nativeLibDir from Android package manager (libaapt2.so)
        if (nativeLibDir != null) {
            val libAapt2 = File(nativeLibDir, "libaapt2.so")
            if (libAapt2.exists() && libAapt2.canExecute()) {
                return libAapt2
            }
        }

        // Priority 2: baseDir/bin/libaapt2.so or baseDir/bin/aapt2
        val binDir = File(baseDir, "bin")
        val binLibAapt2 = File(binDir, "libaapt2.so")
        if (binLibAapt2.exists()) {
            binLibAapt2.setExecutable(true, false)
            return binLibAapt2
        }
        val binAapt2 = File(binDir, "aapt2")
        if (binAapt2.exists()) {
            binAapt2.setExecutable(true, false)
            return binAapt2
        }

        // Priority 3: Custom environment override
        val envAapt2 = System.getenv("AAPT2_PATH")
        if (!envAapt2.isNullOrBlank()) {
            val f = File(envAapt2)
            if (f.exists() && f.canExecute()) return f
        }

        // Priority 4: Development / Host machine SDK fallback
        val userHome = System.getProperty("user.home") ?: ""
        val sdkBuildTools = File(userHome, "Library/Android/sdk/build-tools")
        if (sdkBuildTools.exists() && sdkBuildTools.isDirectory) {
            val versions = sdkBuildTools.listFiles()?.filter { it.isDirectory }?.sortedByDescending { it.name }
            versions?.forEach { versionDir ->
                val hostAapt2 = File(versionDir, "aapt2")
                if (hostAapt2.exists() && hostAapt2.canExecute()) {
                    return hostAapt2
                }
            }
        }

        return null
    }

    fun isCompilerReady(): Boolean {
        val hasAapt2 = getAapt2Executable() != null
        val hasAndroidJar = androidJarFile.exists() && androidJarFile.length() > 0
        val hasKeystore = debugKeystoreFile.exists() && debugKeystoreFile.length() > 0
        return hasAapt2 && hasAndroidJar && hasKeystore
    }
}
