---
phase: 01-standalone-compiler
plan: 01
type: execute
tracer_first: true
---

# Plan: Standalone On-Device APK Compiler & Testbench

## Objective
Extract and package the on-device 5-stage APK compilation engine into a standalone, modular Android library (`:compiler-engine`) with an interactive Compose testbench demo application (`:app`), verified by automated unit tests, and pushed to the personal GitHub account (`Yashwant00CR7/android-apk-compiler`).

## Requirements Addressed
- REQ-01, REQ-02, REQ-03, REQ-04, REQ-05, REQ-06, REQ-07

## Execution Waves

### Wave 1: Gradle & Multi-Module Architecture Scaffolding (Tracer Seam)
- **Task 1.1**: Scaffold root Gradle build scripts, settings, gradle wrapper, and module structure (`:compiler-engine` and `:app`).
- **Task 1.2**: Copy and link native compiler assets: ARM64 `libaapt2.so`, platform `android.jar`, and `debug.keystore` into `:compiler-engine`.

### Wave 2: Engine Implementation & Unit Test Verification
- **Task 2.1**: Implement `ApkCompilerEngine`, `CompilerBootstrapManager`, `CompilerErrorParser`, and `ApkInstallerHelper` in `:compiler-engine`.
- **Task 2.2**: Implement comprehensive unit test in `:compiler-engine` validating end-to-end tracer compilation and APK verification. Run `./gradlew :compiler-engine:testDebugUnitTest`.

### Wave 3: Testbench Demo App UI & Build
- **Task 3.1**: Build interactive Jetpack Compose UI in `:app` with preset Sleek Calculator project, stage progress indicators, log drawer, and one-tap install card.
- **Task 3.2**: Verify full app compilation via `./gradlew :app:assembleDebug`.

### Wave 4: Open Source Documentation & Personal GitHub Push
- **Task 4.1**: Create Apache 2.0 LICENSE and comprehensive README.md with architecture sequence diagram and Gradle integration guide.
- **Task 4.2**: Configure local git personal identity (`Yashwant00CR7` / `10a19yashwant@gmail.com`). Create GitHub repository via `gh` or personal API. Verify pre-push rules and push `main` branch to `git@github-personal:Yashwant00CR7/android-apk-compiler.git`.
