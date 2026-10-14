# Phase Context: Standalone On-Device APK Compiler

## Problem & User Intent
Provide a 100% self-contained, on-device Android compilation engine packaged as a standalone, modular Android library (`:compiler-engine`) with an interactive demo app (`:app`). This enables any Android application or mobile IDE/agent to compile full native APKs from Java/XML source code directly on physical phones without needing a PC or remote cloud server.

## Mental Model & Core Abstractions
1. **Compilation Stages:**
   - Stage 1: Resource Packaging (AAPT2 compile & link against `android.jar`)
   - Stage 2: Java Source Compilation (Eclipse Compiler for Java - ECJ)
   - Stage 3: Bytecode Dexing (Android D8/R8 dexer)
   - Stage 4: Archive Alignment (4-byte boundary ZipAlign)
   - Stage 5: Cryptographic Signing (Apksig v1 + v2 with debug keystore)
2. **Library Boundary (`:compiler-engine`):**
   - Independent of host app UI/architecture.
   - Clean API surface: `ApkCompilerEngine.compileProject(projectDir: File, onProgress: (CompilationProgress) -> Unit): CompilationResult`.
   - Bundles native ARM64 AAPT2 binary, API 35 `android.jar`, and debug keystore assets.
   - Self-bootstraps assets into private app storage (`filesDir/compiler/`) with executable permissions.
3. **Testbench Demo App (`:app`):**
   - Sleek Compose UI with a preset project (Sleek Calculator).
   - Real-time animated stage stepper (AAPT2 -> ECJ -> D8 -> ZipAlign -> Apksig).
   - Log drawer showing stdout/stderr and diagnostic error cards with file/line navigation.
   - One-tap APK installation via `FileProvider` and Android `PackageInstaller`.

## Verification & Backpressure Seams
- **Library Unit Tests:** `./gradlew :compiler-engine:testDebugUnitTest` verifying end-to-end tracer project compilation into a signed, valid APK.
- **Demo App Compilation:** `./gradlew :app:assembleDebug` ensuring clean build of the testbench app with bundled engine library.
- **Git Push Verification:** `git config user.email` returns `10a19yashwant@gmail.com` and `origin` uses `github-personal`.
