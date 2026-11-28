---
last_mapped_commit: 178eeade16d92a0598e3d1178742d6bf8e72edfd
last_mapped_at: 2026-11-27
---
# Codebase Structure

**Analysis Date:** 2026-11-27

## Directory Layout

```
JuCalendar/                           # Repo root (Gradle root project "KalenderWecker")
├── app/                              # Client applications and shared UI
│   ├── androidApp/                   # Android application module (com.android.application)
│   ├── desktopApp/                   # Compose Desktop (JVM) application module
│   ├── iosApp/                       # Xcode project + SwiftUI host (not a Gradle module)
│   ├── shared/                       # Compose Multiplatform shared UI library (KMP)
│   └── webApp/                       # Compose Web app (JS + Wasm executables)
├── core/                             # Pure KMP domain library (ICS parsing, models)
│   ├── karma.config.d/               # Karma config snippets for JS/Wasm browser tests
│   └── src/
│       ├── commonMain/kotlin/        # Shared domain code
│       └── commonTest/               # Shared unit tests and test resources
├── server/                           # Ktor (Netty) JVM server
│   └── src/
│       ├── main/kotlin/              # Server entry point and routing
│       ├── main/resources/           # logback.xml
│       └── test/kotlin/              # Ktor testApplication tests
├── gradle/                           # Version catalog and wrapper
│   ├── libs.versions.toml            # Single source of dependency and plugin versions
│   └── wrapper/                      # Gradle wrapper jar and properties
├── .planning/                        # GSD planning artifacts (codebase maps live here)
│   └── codebase/                     # ARCHITECTURE.md, STRUCTURE.md, etc.
├── .run/                             # IDE run configurations (for example desktop hot run)
├── .idea/                            # IntelliJ project settings
├── .claude/                          # Claude Code settings and worktrees
├── kotlin-js-store/                  # Generated Kotlin/JS npm lock state (committed)
├── build.gradle.kts                  # Root build: declares plugins with apply false
├── settings.gradle.kts               # Included modules and repository config
├── gradle.properties                 # Gradle/Kotlin/Android JVM flags
├── gradlew, gradlew.bat              # Gradle wrapper scripts
├── flake.nix, flake.lock, .envrc     # Nix dev shell (JDK 25, Gradle 9, Kotlin, bun)
├── local.properties                  # Local SDK paths (machine-specific)
├── README.md                         # Project overview and run commands
└── Stuff.md                          # Scratch notes (contains an external ICS feed URL)
```

Build outputs (`build/`, `.gradle/`, `.kotlin/`, `.direnv/`, `hs_err_pid*.log`) are generated and ignored by `.gitignore`. Do not place source in them.

## Directory Purposes

**`app/shared/`:**
- Purpose: The one Compose Multiplatform UI module. Every client depends on it.
- Contains: Shared composables and the `Platform` expect/actual pair, plus per-target source sets.
- Key files:
  - `app/shared/src/commonMain/kotlin/at/zanko/kalenderwecker/App.kt` (root `App()` composable)
  - `app/shared/src/commonMain/kotlin/at/zanko/kalenderwecker/InfiniteScrollList.kt` (lazy list with load-more callback)
  - `app/shared/src/commonMain/kotlin/at/zanko/kalenderwecker/Platform.kt` (expect declaration)
  - `app/shared/src/commonMain/composeResources/drawable/` (Compose resource images)
- Source sets: `commonMain`, `commonTest`, `androidMain`, `androidHostTest`, `iosMain`, `iosTest`, `jvmMain`, `jvmTest`, `jsMain`, `wasmJsMain`, `webTest`.

**`app/androidApp/`:**
- Purpose: Android launcher application.
- Contains: `MainActivity`, Android resources, manifest, mipmap icons.
- Key files:
  - `app/androidApp/src/main/kotlin/at/zanko/kalenderwecker/MainActivity.kt`
  - `app/androidApp/src/main/AndroidManifest.xml`, `app/androidApp/src/main/res/values/`, `app/androidApp/src/main/res/drawable*/`, `app/androidApp/src/main/res/mipmap-*/`
- Namespace: `at.zanko.kalenderwecker`. applicationId `at.zanko.kalenderwecker`.

**`app/desktopApp/`:**
- Purpose: Desktop (JVM) Compose application. Packaging targets are DMG, MSI, and DEB.
- Contains: One `main.kt` and a nested Gradle wrapper pointer.
- Key files:
  - `app/desktopApp/src/main/kotlin/at/zanko/kalenderwecker/main.kt`
  - `app/desktopApp/build.gradle.kts` (`compose.desktop.application` with `mainClass = "at.zanko.kalenderwecker.MainKt"`)
  - `app/desktopApp/gradle/wrapper/gradle-wrapper.properties` (untracked, nested wrapper config)
  - `app/desktopApp/.gradle/` and `app/desktopApp/build/` are generated.

**`app/iosApp/`:**
- Purpose: Xcode project that embeds the Kotlin `Shared` framework produced by `app/shared`.
- Contains: Swift sources, asset catalogs, Xcode project files.
- Key files:
  - `app/iosApp/iosApp/iOSApp.swift` (`@main` app)
  - `app/iosApp/iosApp/ContentView.swift` (wraps `MainViewControllerKt.MainViewController()`)
  - `app/iosApp/iosApp.xcodeproj/` (project file)
  - `app/iosApp/Configuration/Config.xcconfig`
  - `app/iosApp/iosApp/Assets.xcassets/` (AppIcon, AccentColor)
- Note: Not a Gradle module. Its Kotlin side is `app/shared/src/iosMain/`.

**`app/webApp/`:**
- Purpose: Browser build of the UI for JS and Wasm.
- Contains: Web entry point and static resources.
- Key files:
  - `app/webApp/src/webMain/kotlin/at/zanko/kalenderwecker/main.kt`
  - `app/webApp/src/webMain/resources/index.html` and `styles.css` (host page and styles for the browser build)

**`core/`:**
- Purpose: Platform-independent domain code. Safe to depend on from every target.
- Contains: ICS parser, event and date models, and a `sayHello` helper.
- Key files:
  - `core/src/commonMain/kotlin/at/zanko/kalenderwecker/ics/BasicIcsParser.kt` (`parse(ics: String): List<CalendarEvent>`)
  - `core/src/commonMain/kotlin/at/zanko/kalenderwecker/ics/CalendarParser.kt` (`fun interface CalendarParser`, not yet implemented)
  - `core/src/commonMain/kotlin/at/zanko/kalenderwecker/ics/DataHandler.kt` (empty placeholder)
  - `core/src/commonMain/kotlin/at/zanko/kalenderwecker/ics/models/CalendarEvent.kt`
  - `core/src/commonMain/kotlin/at/zanko/kalenderwecker/ics/models/IcsDateValue.kt`
  - `core/src/commonMain/kotlin/at/zanko/kalenderwecker/GreetingUtil.kt` (`sayHello`)
  - `core/src/commonTest/kotlin/at/zanko/kalenderwecker/BasicIcsParserTest.kt`
  - `core/src/commonTest/kotlin/at/zanko/kalenderwecker/TestUtils.kt` (imports okio `FileSystem`, no helpers yet)
  - `core/src/commonTest/resources/` (ICS fixtures `TimeEdit_Short.ics`, `TimeEdit_UltraShort.ics`, and a longer TimeEdit export, loaded via `com.goncalossilva.resources.Resource`)
- Targets: iOS arm64 and simulator arm64, JVM, JS browser, Wasm browser, Android.

**`server/`:**
- Purpose: Ktor HTTP server on the JVM.
- Contains: Application entry point, routing, tests, and resources.
- Key files:
  - `server/src/main/kotlin/at/zanko/kalenderwecker/Application.kt` (`main()` and `Application.module()`)
  - `server/src/main/resources/logback.xml` (Logback configuration)
  - `server/src/test/kotlin/at/zanko/kalenderwecker/ApplicationTest.kt`
  - `server/src/test/kotlin/at/zanko/kalenderwecker/PortConfigTest.kt` (untracked; documents the hardcoded port)
  - `server/build.gradle.kts` (`mainClass = "at.zanko.kalenderwecker.ApplicationKt"`, Ktor plugin)

**`gradle/`:**
- Purpose: Shared dependency and plugin catalog for all modules.
- Key files:
  - `gradle/libs.versions.toml` (versions, libraries, plugins)
  - `gradle/gradle-daemon-jvm.properties` (JVM toolchain request)
  - `gradle/wrapper/gradle-wrapper.properties`

**`.planning/`:**
- Purpose: Planning and codebase-mapping artifacts used by GSD commands.
- Key files: `.planning/codebase/` (this document set).

## Key File Locations

**Entry Points:**
- `app/androidApp/src/main/kotlin/at/zanko/kalenderwecker/MainActivity.kt`: Android activity hosting `App()`.
- `app/desktopApp/src/main/kotlin/at/zanko/kalenderwecker/main.kt`: Desktop `main`.
- `app/webApp/src/webMain/kotlin/at/zanko/kalenderwecker/main.kt`: Web `main` via `ComposeViewport`.
- `app/shared/src/iosMain/kotlin/at/zanko/kalenderwecker/MainViewController.kt`: iOS `MainViewController()` factory.
- `app/iosApp/iosApp/iOSApp.swift`: SwiftUI `@main`.
- `server/src/main/kotlin/at/zanko/kalenderwecker/Application.kt`: Server `main()`.

**Configuration:**
- `settings.gradle.kts`: Included modules (`:app:androidApp`, `:app:desktopApp`, `:app:shared`, `:app:webApp`, `:core`, `:server`).
- `build.gradle.kts`: Root plugin declarations with `apply false`.
- `gradle.properties`: Build JVM args, configuration cache, build cache, `android.useAndroidX`.
- `gradle/libs.versions.toml`: Single version catalog (Kotlin 2.4.21, Compose Multiplatform 1.12.2, Ktor 3.6.1, kotlinx-datetime 0.8.0, Koin 4.2.2, and others).
- `flake.nix`: Nix dev shell.
- `.envrc`: direnv hook for the flake.

**Core Logic:**
- `core/src/commonMain/kotlin/at/zanko/kalenderwecker/ics/BasicIcsParser.kt`: ICS parsing.
- `core/src/commonMain/kotlin/at/zanko/kalenderwecker/ics/models/`: Domain model.
- `app/shared/src/commonMain/kotlin/at/zanko/kalenderwecker/`: Shared UI.

**Testing:**
- `core/src/commonTest/kotlin/at/zanko/kalenderwecker/`: Core unit tests (kotlin.test).
- `app/shared/src/commonTest/kotlin/at/zanko/kalenderwecker/SharedCommonTest.kt`: Template test.
- `app/shared/src/jvmTest/`, `app/shared/src/iosTest/`, `app/shared/src/webTest/`, `app/shared/src/androidHostTest/`: Platform test stubs (template).
- `server/src/test/kotlin/at/zanko/kalenderwecker/`: Ktor `testApplication` tests.

## Naming Conventions

**Files:**
- Kotlin source files use PascalCase class-style names: `App.kt`, `InfiniteScrollList.kt`, `CalendarEvent.kt`.
- Platform-specific files use the suffix `.<platform>.kt`: `Platform.android.kt`, `Platform.ios.kt`, `Platform.jvm.kt`, `Platform.js.kt`, `Platform.wasmJs.kt`.
- Test files end in `Test.kt`: `BasicIcsParserTest.kt`, `ApplicationTest.kt`, `PortConfigTest.kt`. Platform-prefixed test classes use the `SharedLogic*Test` pattern, for example `SharedLogicDesktopTest.kt`.
- Main entry files for JVM and JS targets are named `main.kt` (lowercase). Android, iOS, and Swift files use PascalCase.
- Gradle build scripts use `build.gradle.kts` in each module, with `settings.gradle.kts` at the root.
- Swift files: `iOSApp.swift`, `ContentView.swift`.

**Directories:**
- Gradle modules are lowercase kebab or camel names mapped through `settings.gradle.kts`, using colon paths like `:app:shared`.
- Kotlin source roots follow the standard `src/<sourceSet>/kotlin/at/zanko/kalenderwecker/` layout.
- Feature subpackages are lowercase: `ics/`, `ics/models/`.

**Packages:**
- Root package is `at.zanko.kalenderwecker` for every module. The core domain adds `ics` and `ics.models`. Shared UI and server use the root package directly.
- Android namespace: `at.zanko.kalenderwecker` (app) and `at.zanko.kalenderwecker.app.shared` (shared library). Core namespace: `at.zanko.kalenderwecker.core`.

**Identifiers:**
- Functions and properties: camelCase (`parseIcsDate`, `eventKey`, `getPlatform`).
- Types: PascalCase (`CalendarEvent`, `IcsDateValue`, `Platform`).
- Constants and Gradle keys: kebab or camelCase in TOML (`android-compileSdk`, `kotlinx-datetime`).

## Where to Add New Code

**New ICS or domain logic (pure, no I/O):**
- Primary code: `core/src/commonMain/kotlin/at/zanko/kalenderwecker/ics/` for parsers, or `core/src/commonMain/kotlin/at/zanko/kalenderwecker/` for other domain code.
- Models: `core/src/commonMain/kotlin/at/zanko/kalenderwecker/ics/models/`.
- Tests: `core/src/commonTest/kotlin/at/zanko/kalenderwecker/`. Put ICS fixtures under `core/src/commonTest/resources/`.

**New shared UI screen or composable:**
- Implementation: `app/shared/src/commonMain/kotlin/at/zanko/kalenderwecker/` (one file per screen, for example `CalendarScreen.kt`). Wire it from `App.kt`.
- Tests: `app/shared/src/commonTest/kotlin/at/zanko/kalenderwecker/`. Platform-only behavior goes in `jvmTest`, `iosTest`, `webTest`, or `androidHostTest`.
- Compose resources: `app/shared/src/commonMain/composeResources/drawable/`.

**New platform-specific API call (`expect`/`actual`):**
- Declaration: `app/shared/src/commonMain/kotlin/at/zanko/kalenderwecker/<Name>.kt`.
- Actuals: `app/shared/src/{androidMain,iosMain,jvmMain,jsMain,wasmJsMain}/kotlin/at/zanko/kalenderwecker/<Name>.<platform>.kt`.

**New HTTP endpoint on the server:**
- Routing: add `get`/`post` blocks inside `Application.module()` in `server/src/main/kotlin/at/zanko/kalenderwecker/Application.kt`. For more than a few routes, split into `server/src/main/kotlin/at/zanko/kalenderwecker/routes/` and install from `module()`.
- Tests: `server/src/test/kotlin/at/zanko/kalenderwecker/` using `testApplication { application { module() } }`.

**New client app or target:**
- Create a module directory under `app/`, add it to `settings.gradle.kts` as `:app:<name>`, depend on `project(":app:shared")`, and keep the entry point thin.

**New dependency:**
- Add the version to `[versions]` and the coordinate to `[libraries]` in `gradle/libs.versions.toml`, then reference it through `libs.*` in the module's `build.gradle.kts`.

**Utilities:**
- Pure shared helpers: `core/src/commonMain/kotlin/at/zanko/kalenderwecker/`.
- UI helpers: `app/shared/src/commonMain/kotlin/at/zanko/kalenderwecker/` (see `InfiniteScrollList.kt` as the existing example).

## Special Directories

**`app/desktopApp/gradle/`:**
- Purpose: Nested Gradle wrapper configuration for the desktop module.
- Generated: No (hand-maintained wrapper properties).
- Committed: Untracked at the time of analysis (`git status` shows `?? app/desktopApp/gradle/`). Decide whether to track it.

**`core/karma.config.d/`:**
- Purpose: Karma configuration overrides for the core module's browser tests (JS and Wasm).
- Generated: No.
- Committed: Untracked at the time of analysis (`git status` shows `?? core/karma.config.d/`).

**`kotlin-js-store/`:**
- Purpose: Kotlin/JS and Wasm npm lock files (`package-lock.json`, `yarn.lock`) managed by the Kotlin Gradle plugin.
- Generated: Yes, by the Kotlin/JS toolchain.
- Committed: Yes (tracked in git).

**`build/`, `app/*/build/`, `core/build/`, `server/build/`:**
- Purpose: Gradle build outputs.
- Generated: Yes.
- Committed: No (ignored by `**/build/` in `.gitignore`).

**`.gradle/`, `core/.gradle/`, `app/desktopApp/.gradle/`, `.kotlin/`:**
- Purpose: Gradle and Kotlin caches.
- Generated: Yes.
- Committed: No.

**`.direnv/`:**
- Purpose: direnv and Nix flake environment cache.
- Generated: Yes.
- Committed: No.

**`.idea/`, `.run/`:**
- Purpose: IntelliJ IDEA project and run configuration. `.run/` holds the shared "app.desktopApp [hot]" run config.
- Generated: Partially (`.idea/` is mostly ignored, `.run/` is tracked).
- Committed: `.run/` is tracked. `.idea/` is ignored by `.gitignore` (`.idea` entry).

**`.claude/`:**
- Purpose: Claude Code project settings and `worktrees/` for isolated checkouts.
- Generated: Partially.
- Committed: Settings are project-specific. Do not commit `.claude/settings.local.json`.

**`hs_err_pid1586.log`:**
- Purpose: JVM crash dump at repo root.
- Generated: Yes.
- Committed: Not tracked (`*.log` is in `.gitignore`). Delete it when it is no longer needed.

---

*Structure analysis: 2026-11-27*
