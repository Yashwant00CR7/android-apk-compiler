package com.yashwant.compiler.demo

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yashwant.compiler.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class CompilerDemoUiState(
    val isCompiling: Boolean = false,
    val currentStage: CompilationStage? = null,
    val progressPercent: Int = 0,
    val statusMessage: String = "Ready to compile on-device.",
    val result: CompilerResult? = null,
    val logs: List<String> = emptyList()
)

class DemoCompilerViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(CompilerDemoUiState())
    val uiState = _uiState.asStateFlow()

    fun compilePresetProject(context: Context) {
        if (_uiState.value.isCompiling) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isCompiling = true,
                currentStage = CompilationStage.INIT,
                progressPercent = 5,
                statusMessage = "Bootstrapping compiler toolchain...",
                result = null,
                logs = listOf("Starting on-device compilation...")
            )

            val finalResult = withContext(Dispatchers.IO) {
                try {
                    val appDataDir = context.filesDir
                    val nativeLibDir = File(context.applicationInfo.nativeLibraryDir)
                    val bootstrapManager = CompilerBootstrapManager(appDataDir, nativeLibDir)

                    // 1. Extract assets
                    bootstrapManager.extractCompilerAssets { path ->
                        try {
                            context.assets.open(path)
                        } catch (_: Exception) {
                            null
                        }
                    }

                    // 2. Scaffold Sleek Calculator project
                    val projectDir = File(appDataDir, "projects/SleekCalculator").apply { mkdirs() }
                    scaffoldSleekCalculator(projectDir)

                    // 3. Compile
                    val engine = ApkCompilerEngine(bootstrapManager)
                    engine.compileProject(projectDir) { progress ->
                        _uiState.value = _uiState.value.copy(
                            currentStage = progress.stage,
                            progressPercent = progress.progressPercent,
                            statusMessage = progress.message,
                            logs = _uiState.value.logs + "[${progress.stage.name}] ${progress.message}"
                        )
                    }
                } catch (e: Exception) {
                    CompilerResult(
                        success = false,
                        failedStage = CompilationStage.FAILED,
                        errorMessage = e.message ?: "Unknown error",
                        rawOutput = e.stackTraceToString()
                    )
                }
            }

            _uiState.value = _uiState.value.copy(
                isCompiling = false,
                currentStage = if (finalResult.success) CompilationStage.COMPLETED else finalResult.failedStage,
                progressPercent = if (finalResult.success) 100 else _uiState.value.progressPercent,
                statusMessage = if (finalResult.success) {
                    "Compilation succeeded in ${finalResult.durationMs}ms!"
                } else {
                    "Compilation failed: ${finalResult.errorMessage}"
                },
                result = finalResult,
                logs = _uiState.value.logs + if (finalResult.success) {
                    "Success: Generated ${finalResult.apkFile?.name} (${finalResult.sizeBytes / 1024} KB)"
                } else {
                    "Failed: ${finalResult.errorMessage}\n${finalResult.rawOutput}"
                }
            )
        }
    }

    fun installApk(context: Context) {
        val apkFile = _uiState.value.result?.apkFile ?: return
        ApkInstallerHelper.installApk(context, apkFile, "${context.packageName}.fileprovider")
    }

    fun shareApk(context: Context) {
        val apkFile = _uiState.value.result?.apkFile ?: return
        ApkInstallerHelper.shareApk(context, apkFile, "${context.packageName}.fileprovider")
    }

    private fun scaffoldSleekCalculator(projectDir: File) {
        // Manifest
        val manifest = File(projectDir, "AndroidManifest.xml")
        manifest.writeText(
            """
            <?xml version="1.0" encoding="utf-8"?>
            <manifest xmlns:android="http://schemas.android.com/apk/res/android"
                package="com.yashwant.sleekcalculator"
                android:versionCode="1"
                android:versionName="1.0">
                <application android:label="SleekCalculator" android:hasCode="true">
                    <activity android:name=".MainActivity" android:exported="true">
                        <intent-filter>
                            <action android:name="android.intent.action.MAIN" />
                            <category android:name="android.intent.category.LAUNCHER" />
                        </intent-filter>
                    </activity>
                </application>
            </manifest>
            """.trimIndent()
        )

        // Resources
        val resValues = File(projectDir, "res/values").apply { mkdirs() }
        File(resValues, "strings.xml").writeText(
            """
            <?xml version="1.0" encoding="utf-8"?>
            <resources>
                <string name="app_name">SleekCalculator</string>
                <string name="title">On-Device Sleek Calculator</string>
            </resources>
            """.trimIndent()
        )

        val resLayout = File(projectDir, "res/layout").apply { mkdirs() }
        File(resLayout, "activity_main.xml").writeText(
            """
            <?xml version="1.0" encoding="utf-8"?>
            <LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
                android:layout_width="match_parent"
                android:layout_height="match_parent"
                android:padding="24dp"
                android:gravity="center"
                android:orientation="vertical">
                <TextView
                    android:id="@+id/resultText"
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:textSize="22sp"
                    android:text="@string/title" />
            </LinearLayout>
            """.trimIndent()
        )

        // Java source
        val srcDir = File(projectDir, "src/com/yashwant/sleekcalculator").apply { mkdirs() }
        File(srcDir, "MainActivity.java").writeText(
            """
            package com.yashwant.sleekcalculator;

            import android.app.Activity;
            import android.os.Bundle;
            import android.widget.TextView;

            public class MainActivity extends Activity {
                @Override
                protected void onCreate(Bundle savedInstanceState) {
                    super.onCreate(savedInstanceState);
                    setContentView(R.layout.activity_main);
                    TextView tv = findViewById(R.id.resultText);
                    if (tv != null) {
                        tv.setText("Compiled on Phone! 🚀 Result: " + (21 * 2));
                    }
                }
            }
            """.trimIndent()
        )
    }
}
