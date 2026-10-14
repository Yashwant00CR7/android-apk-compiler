package com.yashwant.compiler

import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.zip.ZipFile

class ApkCompilerEngineTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var appDataDir: File
    private lateinit var projectDir: File
    private lateinit var bootstrapManager: CompilerBootstrapManager
    private lateinit var engine: ApkCompilerEngine

    @Before
    fun setUp() {
        appDataDir = tempFolder.newFolder("app_data")
        projectDir = tempFolder.newFolder("SleekCalculator")

        bootstrapManager = CompilerBootstrapManager(appDataDir)
        bootstrapManager.initialize()

        // Copy android.jar from host SDK or bundled asset for testing
        val userHome = System.getProperty("user.home") ?: ""
        val sdkJar = File(userHome, "Library/Android/sdk/platforms/android-35/android.jar")
        if (sdkJar.exists()) {
            sdkJar.copyTo(bootstrapManager.androidJarFile, overwrite = true)
        } else {
            val bundledJar = File("src/main/assets/compiler/android.jar")
            if (bundledJar.exists()) {
                bundledJar.copyTo(bootstrapManager.androidJarFile, overwrite = true)
            }
        }

        // Copy debug.keystore for unit testing
        val sdkKeystore = File(userHome, ".android/debug.keystore")
        if (sdkKeystore.exists()) {
            sdkKeystore.copyTo(bootstrapManager.debugKeystoreFile, overwrite = true)
        } else {
            val bundledKeystore = File("src/main/assets/compiler/debug.keystore")
            if (bundledKeystore.exists()) {
                bundledKeystore.copyTo(bootstrapManager.debugKeystoreFile, overwrite = true)
            }
        }

        engine = ApkCompilerEngine(bootstrapManager)
    }

    @Test
    fun testCompileTracerProjectSuccess() {
        val aapt2 = bootstrapManager.getAapt2Executable()
        if (aapt2 == null || !bootstrapManager.androidJarFile.exists()) {
            System.err.println("Skipping testCompileTracerProjectSuccess: host AAPT2 or android.jar not found.")
            return
        }

        // 1. Scaffold AndroidManifest.xml
        val manifestFile = File(projectDir, "AndroidManifest.xml")
        manifestFile.writeText(
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

        // 2. Scaffold resources
        val resValues = File(projectDir, "res/values").apply { mkdirs() }
        File(resValues, "strings.xml").writeText(
            """
            <?xml version="1.0" encoding="utf-8"?>
            <resources>
                <string name="app_name">SleekCalculator</string>
                <string name="title">Simple Calculator</string>
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
                android:orientation="vertical">
                <TextView
                    android:id="@+id/resultText"
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="@string/title" />
            </LinearLayout>
            """.trimIndent()
        )

        // 3. Scaffold Java Source
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
                        tv.setText("Calculator 42");
                    }
                }

                public int add(int a, int b) {
                    return a + b;
                }
            }
            """.trimIndent()
        )

        val progressList = mutableListOf<CompilationProgress>()
        val result = engine.compileProject(projectDir) { progress ->
            progressList.add(progress)
        }

        assertTrue("Expected compilation to succeed. Output: ${result.errorMessage} | ${result.rawOutput}", result.success)
        assertNotNull(result.apkFile)
        assertTrue(result.apkFile!!.exists())
        assertTrue(result.sizeBytes > 1000)
        assertEquals("com.yashwant.sleekcalculator", result.packageName)
        assertEquals("SleekCalculator", result.appName)

        // Verify APK contents
        ZipFile(result.apkFile!!).use { zip ->
            assertNotNull("APK missing AndroidManifest.xml", zip.getEntry("AndroidManifest.xml"))
            assertNotNull("APK missing classes.dex", zip.getEntry("classes.dex"))
            assertNotNull("APK missing resources.arsc", zip.getEntry("resources.arsc"))
            val metaInfEntries = zip.entries().asSequence().filter { it.name.startsWith("META-INF/") }.toList()
            assertTrue("APK missing signature in META-INF", metaInfEntries.isNotEmpty())
        }

        assertTrue("Expected progress callbacks", progressList.isNotEmpty())
        assertEquals(CompilationStage.COMPLETED, progressList.last().stage)
    }

    @Test
    fun testCompileFailsOnJavaSyntaxError() {
        val aapt2 = bootstrapManager.getAapt2Executable()
        if (aapt2 == null || !bootstrapManager.androidJarFile.exists()) {
            return
        }

        File(projectDir, "AndroidManifest.xml").writeText(
            """
            <manifest xmlns:android="http://schemas.android.com/apk/res/android" package="com.example.broken">
                <application />
            </manifest>
            """.trimIndent()
        )
        val srcDir = File(projectDir, "src/com/example/broken").apply { mkdirs() }
        File(srcDir, "Broken.java").writeText(
            """
            package com.example.broken;
            public class Broken {
                invalid syntax here !!!
            }
            """.trimIndent()
        )

        val result = engine.compileProject(projectDir)
        assertFalse(result.success)
        assertEquals(CompilationStage.JAVA_COMPILATION, result.failedStage)
        assertTrue(result.errors.isNotEmpty())
    }
}
