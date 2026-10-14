# Requirements: Standalone On-Device APK Compiler

- [ ] **REQ-01**: Multi-module Gradle build (`:compiler-engine` Android library + `:app` demo application) targeting Android SDK 35 (minSdk 26).
- [ ] **REQ-02**: Modular `:compiler-engine` containing the 5-stage `ApkCompilerEngine`, `CompilerBootstrapManager`, `CompilerErrorParser`, and `ApkInstallerHelper`.
- [ ] **REQ-03**: Bundled native toolchain: Statically linked ARM64 `libaapt2.so`, platform `android.jar` (API 35), and `debug.keystore`.
- [ ] **REQ-04**: Interactive demo testbench Compose app (`:app`) featuring a preset Sleek Calculator project, stage progress visualizer, diagnostic log inspector, and one-tap APK installation.
- [ ] **REQ-05**: Automated unit testing in `:compiler-engine` validating end-to-end tracer compilation producing a signed, valid APK with manifest, DEX, and resources.
- [ ] **REQ-06**: Production-ready open-source documentation (README.md with architecture sequence diagram and quickstart guide, LICENSE with Apache 2.0).
- [ ] **REQ-07**: Initialize Git repository with personal account identity (`Yashwant00CR7` / `10a19yashwant@gmail.com`), create remote on personal GitHub, and push via `git@github-personal:Yashwant00CR7/android-apk-compiler.git`.
