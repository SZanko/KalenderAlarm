---
last_mapped_commit: 178eeade16d92a0598e3d1178742d6bf8e72edfd
last_mapped_at: 2026-11-27
---
# Technology Stack

**Analysis Date:** 2026-11-27

## Languages

**Primary:**
- Kotlin 2.4.21 (`gradle/libs.versions.toml`, `kotlin = "2.4.21"`) - all modules: `core/`, `app/shared/`, `app/androidApp/`, `app/desktopApp/`, `app/webApp/`, `server/`

**Secondary:**
- Gradle Kotlin DSL (`*.gradle.kts`) - build scripts in `build.gradle.kts`, `settings.gradle.kts`, and each module's `build.gradle.kts`
- HTML/CSS - web host page `app/webApp/src/webMain/resources/index.html` and `app/webApp/src/webMain/resources/styles.css`
- Nix - dev shell definition in `flake.nix`
- XML - Android manifest `app/androidApp/src/main/AndroidManifest.xml`, Compose drawables under `app/shared/src/commonMain/composeResources/`, logging config `server/src/main/resources/logback.xml`
- Swift/iOS - referenced by `app/iosApp/` in `README.md`, with `app/shared/src/iosMain/kotlin/at/zanko/kalenderwecker/MainViewController.kt` as the Kotlin entry point. The `app/iosApp/` directory is not present in the working tree.

## Runtime

**Environments:**
- JVM 25 targeted by the Nix dev shell (`flake.nix`, `javaVersion = 25`, `jdk25`)
- JVM toolchain 21 requested via Gradle toolchain properties (`gradle/gradle-daemon-jvm.properties`: `toolchainVersion=21`, vendor AZUL, provisioned via the foojay resolver plugin in `settings.gradle.kts`)
- JVM bytecode target 11 for Kotlin compilation in `core/build.gradle.kts`, `app/shared/build.gradle.kts`, `app/androidApp/build.gradle.kts` (`JVM_11`)
- Android: `minSdk` 31, `compileSdk` 37, `targetSdk` 37 (`gradle/libs.versions.toml`)
- JavaScript (browser) and Kotlin/Wasm (browser) via Kotlin Multiplatform
- Native iOS (`iosArm64`, `iosSimulatorArm64`) declared in `core/build.gradle.kts` and `app/shared/build.gradle.kts`

**Package Manager:**
- Gradle 9.8.2 via wrapper (`gradle/wrapper/gradle-wrapper.properties`, `gradlew`, `gradlew.bat`)
- Also `gradle_9` in the Nix shell (`flake.nix`)
- Secondary wrapper at `app/desktopApp/gradle/wrapper/gradle-wrapper.properties` (untracked, see `git status`)
- Bun is included in the Nix dev shell (`flake.nix`) but no `package.json` exists in the repo
- Lockfile: Gradle dependency locking not configured. Nix lock present as `flake.lock`. Kotlin/JS lock store at `kotlin-js-store/` (generated).

## Frameworks

**Core:**
- Kotlin Multiplatform (KMP) - shared code across Android, iOS, JVM, JS, and Wasm; module layout in `settings.gradle.kts`
- Compose Multiplatform 1.12.2 (`composeMultiplatform` in `gradle/libs.versions.toml`) - UI in `app/shared/src/commonMain/kotlin/at/zanko/kalenderwecker/App.kt`
- Material3 1.12.0-alpha03 (`compose-material3`) - UI components
- Ktor 3.6.1 (`ktor`) - server framework in `server/src/main/kotlin/at/zanko/kalenderwecker/Application.kt` (Netty engine, `io.ktor.plugin` Gradle plugin)
- Koin 4.2.2 (`koin`) - DI library declared in the version catalog. No `koin` imports exist in source yet.
- AndroidX Activity 1.13.0, Lifecycle 2.11.0 (`androidx-activity`, `androidx-lifecycle`) - `app/androidApp/src/main/kotlin/at/zanko/kalenderwecker/MainActivity.kt`, `app/shared/.../App.kt`

**Testing:**
- kotlin-test (`kotlin-test`, `kotlin-test-junit`) 2.4.21 - all test source sets
- JUnit 4.13.2 (`junit`) - JVM test runner
- MockK 1.14.11 (`mockk`, `mockk-bdd`, `mockk-android`) - declared in the version catalog, no usage found in the source tree
- Ktor server test host (`ktor-server-test-host`) - `server/src/test/kotlin/at/zanko/kalenderwecker/ApplicationTest.kt`, `PortConfigTest.kt`
- AndroidX test (`androidx-testExt-junit` 1.3.0, `androidx-espresso-core` 3.7.0, `androidx-testRunner` 1.7.0) - Android instrumented tests
- Karma test proxy configs in `core/karma.config.d/` (JS and Wasm test runners)

**Build/Dev:**
- Android Gradle Plugin 9.5.1 (`agp`) - `androidApplication`, `androidMultiplatformLibrary`
- Compose Compiler plugin (`org.jetbrains.kotlin.plugin.compose`, Kotlin 2.4.21)
- Compose Multiplatform Gradle plugin (`org.jetbrains.compose`) - desktop packaging in `app/desktopApp/build.gradle.kts` (`dmg`, `msi`, `deb`)
- kotlinx-resources plugin 0.16.2 (`com.goncalossilva.resources`) - resource loading in `core/build.gradle.kts` (test)
- Foojay toolchain resolver 1.0.0 (`org.gradle.toolchains.foojay-resolver-convention`) - `settings.gradle.kts`
- Nix flake (`flake.nix`, nixpkgs unstable, formatter `nixfmt`) plus `.envrc` (`use flake`) for direnv

## Key Dependencies

**Critical:**
- `org.jetbrains.kotlinx:kotlinx-datetime` 0.8.0 - date/time model for ICS parsing (`core/src/commonMain/kotlin/at/zanko/kalenderwecker/ics/models/IcsDateValue.kt`, `ics/BasicIcsParser.kt`)
- `kotlin.time.Instant` (stdlib) - instant type used in `core/src/commonMain/kotlin/at/zanko/kalenderwecker/ics/models/CalendarEvent.kt`
- `com.squareup.okio:okio` 3.18.2 - declared as a `commonMain` dependency of `core/`. No `okio` imports found in current source.
- `io.ktor:ktor-server-core-jvm` and `ktor-server-netty-jvm` 3.6.1 - the only HTTP stack in production code (`server/`)
- `ch.qos.logback:logback-classic` 1.6.6 - server logging, configured by `server/src/main/resources/logback.xml` (root level `trace`, Jetty and Netty loggers at INFO)
- `org.jetbrains.kotlinx:kotlinx-coroutines-swing` 1.11.0 - desktop coroutine dispatcher (`app/desktopApp/build.gradle.kts`)

**Infrastructure:**
- `org.jetbrains.kotlin-wrappers:kotlin-browser` 2026.11.0 - JS browser bindings, `jsMain` in `app/shared/build.gradle.kts`
- `io.insert-koin:koin-*` 4.2.2 - declared (`koin-core`, `koin-compose`, `koin-compose-viewmodel`, `koin-android`, `koin-android-compose`, `koin-android-compose-viewmodel`), not referenced by any source file yet
- `com.goncalossilva:resources` 0.16.2 - test resource access in `core/src/commonTest/`
- `org.jetbrains.compose.components:components-resources` - Compose resource API (`Res.drawable.compose_multiplatform` in `App.kt`)
- Gradle property overrides in `gradle.properties`: `kotlin.code.style=official`, `org.gradle.configuration-cache=true`, `org.gradle.caching=true`, `android.useAndroidX=true`, `android.nonTransitiveRClass=true`

## Module Layout

| Gradle path | Directory | Targets | Entry point |
|---|---|---|---|
| `:core` | `core/` | common, iosArm64, iosSimulatorArm64, jvm, js, wasmJs, android library | `core/src/commonMain/kotlin/at/zanko/kalenderwecker/ics/BasicIcsParser.kt` |
| `:app:shared` | `app/shared/` | common, iOS framework `Shared` (static), jvm, js (executable), wasmJs (executable), android library | `app/shared/src/commonMain/kotlin/at/zanko/kalenderwecker/App.kt` |
| `:app:androidApp` | `app/androidApp/` | Android application (`at.zanko.kalenderwecker`) | `app/androidApp/src/main/kotlin/at/zanko/kalenderwecker/MainActivity.kt` |
| `:app:desktopApp` | `app/desktopApp/` | JVM desktop (Compose Desktop) | `app/desktopApp/src/main/kotlin/at/zanko/kalenderwecker/main.kt` |
| `:app:webApp` | `app/webApp/` | js, wasmJs browser executables | `app/webApp/src/webMain/kotlin/at/zanko/kalenderwecker/main.kt` |
| `:server` | `server/` | JVM (Ktor Netty), application `at.zanko.kalenderwecker.ApplicationKt` | `server/src/main/kotlin/at/zanko/kalenderwecker/Application.kt` |

Note: `app/iosApp/` is referenced in `settings.gradle.kts` context and `README.md`, but is not a Gradle module. The iOS framework is produced by `:app:shared`.

## Configuration

**Environment:**
- No environment variables are read by production code. `server/src/main/kotlin/at/zanko/kalenderwecker/Application.kt` hardcodes the port (`port = 8080`) and host (`host = "0.0.0.0"`).
- `server/src/test/kotlin/at/zanko/kalenderwecker/PortConfigTest.kt` documents that port configuration via environment variable is not implemented.
- `.envrc` contains `use flake` (direnv loads the Nix shell).
- `local.properties` exists locally (Android SDK location, git-ignored). Contents are not recorded here.
- No `.env` files detected.

**Build:**
- `settings.gradle.kts` - project includes and plugin repositories (Google, Maven Central, Gradle Plugin Portal)
- `build.gradle.kts` - root plugin declarations (`apply false`)
- `gradle/libs.versions.toml` - single version catalog for all versions and libraries
- `gradle.properties` - JVM args (`-Xmx4096M`), configuration cache, build cache
- `gradle/gradle-daemon-jvm.properties` - JVM toolchain provisioning map (AZUL 21 per platform)
- `flake.nix` / `flake.lock` - reproducible dev shell (JDK 25, Gradle 9, Kotlin, gcc, zlib, bun)
- `app/androidApp/proguard-rules.pro` - ProGuard rules (minify disabled in release)
- `.gitignore` - excludes `build/`, `.gradle`, `.kotlin`, `.idea`, `local.properties`, `*.log`, `node_modules/`, `.direnv/`

## Platform Requirements

**Development:**
- JDK 21 (Gradle toolchain) or JDK 25 (Nix shell), Gradle 9.x via wrapper
- Android SDK (API 37), via `local.properties` or `ANDROID_HOME`
- Xcode for iOS targets (`README.md`)
- Browser with Wasm GC support for `wasmJsBrowserDevelopmentRun`; JS target for older browsers (`README.md`)

**Production:**
- Android: `at.zanko.kalenderwecker`, minSdk 31, release build not minified
- Desktop: native packages DMG (macOS), MSI (Windows), DEB (Linux) via `compose.desktop` (`app/desktopApp/build.gradle.kts`), package version 1.0.0
- Web: static files `index.html`, `styles.css`, and compiled `webApp.js` (JS) or Wasm output
- Server: JVM application listening on `0.0.0.0:8080`, started with `./gradlew :server:run`. No Dockerfile, Procfile, or deployment manifest present.

---

*Stack analysis: 2026-11-27*
