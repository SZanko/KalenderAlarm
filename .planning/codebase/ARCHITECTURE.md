---
last_mapped_commit: 178eeade16d92a0598e3d1178742d6bf8e72edfd
last_mapped_at: 2026-11-27
---
<!-- refreshed: 2026-11-27 -->
# Architecture

**Analysis Date:** 2026-11-27

## System Overview

```text
┌──────────────────────────────────────────────────────────────────────────┐
│                         Client Entry Points                              │
│  Android: `app/androidApp/.../MainActivity.kt`                           │
│  iOS:     `app/iosApp/iosApp/iOSApp.swift` + `ContentView.swift`         │
│  Desktop: `app/desktopApp/.../main.kt`                                   │
│  Web:     `app/webApp/src/webMain/.../main.kt` (JS and Wasm)             │
│  Server:  `server/src/main/.../Application.kt` (no UI)                   │
└───────────────┬───────────────────────────────────────┬──────────────────┘
                │ App() / MainViewController()          │ sayHello()
                ▼                                       │
┌──────────────────────────────────────────────┐        │
│  Shared UI Module  `app/shared`              │        │
│  commonMain: App.kt, Greeting.kt,            │        │
│  InfiniteScrollList.kt, Platform.kt          │        │
│  actuals: Platform.{android,ios,jvm,js,      │        │
│           wasmJs}.kt                         │        │
└─────────────────────┬────────────────────────┘        │
                      │ api(project(":core"))           │
                      ▼                                 ▼
┌─────────────────────────────────────────────────────────────────────────┐
│  Core Domain Module  `core`                                              │
│  commonMain: GreetingUtil.kt (sayHello)                                  │
│              ics/BasicIcsParser.kt  (parse)                              │
│              ics/CalendarParser.kt  (fun interface, unused)              │
│              ics/models/CalendarEvent.kt, IcsDateValue.kt               │
│              ics/DataHandler.kt  (empty placeholder)                     │
└─────────────────────────────────────────────────────────────────────────┘
```

Build-time graph (Gradle project dependencies, from `settings.gradle.kts`):

```text
:app:androidApp ─┐
:app:desktopApp ─┼──► :app:shared ──api──► :core
:app:webApp     ─┘
:server ──────────────────────────────────api──► :core
```

## Current State (important for planners)

The codebase is at an early stage. The multiplatform scaffolding and the core ICS parsing
pieces exist, but the runtime wiring between them does not yet:

- The UI in `app/shared/src/commonMain/kotlin/at/zanko/kalenderwecker/App.kt` is the default
  Compose Multiplatform template (a "Click me!" button and a greeting). It does not use
  `core`'s ICS code.
- The Ktor server in `server/src/main/kotlin/at/zanko/kalenderwecker/Application.kt` exposes only
  `GET /`, which responds with `sayHello("Ktor")` from `core`.
- `core/src/commonMain/kotlin/at/zanko/kalenderwecker/ics/BasicIcsParser.kt` is the only real
  domain logic. It is exercised only by `core/src/commonTest/kotlin/at/zanko/kalenderwecker/BasicIcsParserTest.kt`.
- No fetching of ICS feeds, no persistence, no state holder, and no navigation exist yet.

Plan new work against these real seams rather than against the README's generic description.

## Pattern Overview

**Overall:** Kotlin Multiplatform monorepo with a shared-core / shared-UI / thin-platform-shell
layering. Business logic lives in a pure Kotlin `core` module with no platform dependencies
beyond `kotlinx-datetime` and `okio`. UI lives in one Compose Multiplatform module that every
client target consumes. Each client module is a thin entry point.

**Key Characteristics:**
- Platform code is isolated with Kotlin `expect`/`actual` declarations, e.g.
  `getPlatform()` in `app/shared/src/commonMain/kotlin/at/zanko/kalenderwecker/Platform.kt`
  with actuals in `app/shared/src/{androidMain,iosMain,jvmMain,jsMain,wasmJsMain}/kotlin/at/zanko/kalenderwecker/Platform.*.kt`.
- All targets share one package root, `at.zanko.kalenderwecker`, with the `ics` subpackage for the
  calendar domain. Packages are not split per module.
- The UI is declarative Compose. The iOS target hosts Compose through a `UIViewController`
  (`app/shared/src/iosMain/kotlin/at/zanko/kalenderwecker/MainViewController.kt`) wrapped by SwiftUI in
  `app/iosApp/iosApp/ContentView.swift`.
- Domain modeling is immutable `data class` / `sealed interface` (see `IcsDateValue`).
- Parsing is pure functions over `String` input. No I/O is performed inside `core`.

## Layers

**Core Domain (`core`):**
- Purpose: Platform-independent calendar domain and parsing.
- Location: `core/src/commonMain/kotlin/at/zanko/kalenderwecker/`
- Contains: ICS parsing (`ics/BasicIcsParser.kt`), event and date models (`ics/models/`), `sayHello` helper (`GreetingUtil.kt`).
- Depends on: `kotlinx-datetime`, `okio` (declared in `core/build.gradle.kts`; `okio` is not yet imported by any main source).
- Used by: `:app:shared` (via `api`), `:server` (via `api`).
- Targets: `iosArm64`, `iosSimulatorArm64`, `jvm`, `js` (browser), `wasmJs` (browser), `android`.

**Shared UI (`app/shared`):**
- Purpose: Single Compose Multiplatform UI and platform abstraction used by every client.
- Location: `app/shared/src/commonMain/kotlin/at/zanko/kalenderwecker/`
- Contains: `App()` composable (`App.kt`), `InfiniteScrollList` composable (`InfiniteScrollList.kt`, not referenced yet), `Greeting` (`Greeting.kt`), `Platform` expect/actual.
- Depends on: `:core` (api), Compose Multiplatform runtime/foundation/material3/ui/resources, `androidx.lifecycle` viewmodel-compose and runtime-compose (declared, no ViewModels exist yet).
- Used by: `:app:androidApp`, `:app:desktopApp`, `:app:webApp`, and the iOS framework named `Shared` (`isStatic = true`).
- Compose resources: `app/shared/src/commonMain/composeResources/drawable` (accessed as `kalenderwecker.app.shared.generated.resources.Res`).

**Client Shells (thin entry points):**
- Android: `app/androidApp/src/main/kotlin/at/zanko/kalenderwecker/MainActivity.kt` (`ComponentActivity` calling `setContent { App() }`).
- Desktop: `app/desktopApp/src/main/kotlin/at/zanko/kalenderwecker/main.kt` (Compose `application {}` with a single `Window`).
- Web: `app/webApp/src/webMain/kotlin/at/zanko/kalenderwecker/main.kt` (`ComposeViewport { App() }`), built for JS and Wasm.
- iOS: `app/iosApp/iosApp/iOSApp.swift` and `ContentView.swift` (SwiftUI `WindowGroup` hosting `MainViewControllerKt.MainViewController()`).

**Server (`server`):**
- Purpose: Ktor/Netty HTTP service intended to serve calendar data to clients.
- Location: `server/src/main/kotlin/at/zanko/kalenderwecker/Application.kt`
- Contains: `main()` (Netty engine on `0.0.0.0:8080`, hardcoded) and `Application.module()` (routing).
- Depends on: `:core` (api), Ktor server core and Netty, Logback.
- Used by: Nothing in the repo yet. It is run via `./gradlew :server:run`.

## Data Flow

### Planned / Existing Core Parsing Path

1. Raw ICS text is supplied as a `String` to `parse(ics)` (`core/src/commonMain/kotlin/at/zanko/kalenderwecker/ics/BasicIcsParser.kt:16`).
2. `unfold()` normalizes CRLF/CR to LF, merges RFC 5545 folded continuation lines, and drops empty lines (`BasicIcsParser.kt`, private `unfold`).
3. The parser checks for `BEGIN:VCALENDAR` and `END:VCALENDAR` with `require`, then walks lines. Within `BEGIN:VEVENT`/`END:VEVENT` it collects properties into a `Map<String, Property>`. `Property` (`value` plus `params`) is produced by `parseProperty()`.
4. On `END:VEVENT`, `createEvent(properties)` builds a `CalendarEvent` (`core/src/commonMain/kotlin/at/zanko/kalenderwecker/ics/models/CalendarEvent.kt`). `eventKey` is `"$uid|${recurrenceId}"` and serves as the identity for recurrence overrides.
5. Date properties go through `parseIcsDate()`, which returns an `IcsDateValue` variant:
   - `Date` for `VALUE=DATE` or 8-digit values.
   - `Utc` for a trailing `Z` (via `kotlin.time.Instant`).
   - `Zoned` when `TZID` is present.
   - `Floating` otherwise.
6. `IcsDateValue.toInstant(floatingTimeZone)` (`core/src/commonMain/kotlin/at/zanko/kalenderwecker/ics/models/IcsDateValue.kt`) converts to an instant when a zone is known. Callers decide how to treat `Date` and `Floating`.
7. The result is a `List<CalendarEvent>`. It is returned to the caller with no caching or side effects.

### Client Startup Path (current)

1. Platform entry (`MainActivity`, desktop `main`, web `main`, or SwiftUI `iOSApp`) creates the window or view.
2. It calls the shared `App()` composable (`app/shared/src/commonMain/kotlin/at/zanko/kalenderwecker/App.kt`).
3. `App()` wraps content in `MaterialTheme`, holds local `remember { mutableStateOf }` state, and renders a button. Greeting text comes from `Greeting().greet()`, which uses `getPlatform()` and `sayHello()`.

### Server Request Path (current)

1. `embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module)` starts in `main()` (`server/src/main/kotlin/at/zanko/kalenderwecker/Application.kt`).
2. `Application.module()` installs routing with a single `get("/")` handler.
3. The handler responds with `sayHello("Ktor")` as plain text.

**State Management:**
- UI state is local to composables (`remember`, `mutableStateOf`). No `ViewModel`, store, or DI container is wired, even though Koin and lifecycle-viewmodel entries exist in `gradle/libs.versions.toml`.
- The server is stateless. The core parser is stateless.

## Key Abstractions

**`CalendarEvent`:**
- Purpose: Immutable value for one VEVENT.
- Examples: `core/src/commonMain/kotlin/at/zanko/kalenderwecker/ics/models/CalendarEvent.kt`
- Pattern: `data class` with nullable optional fields (`description`, `location`, `end`, `recurrenceId`, `lastModified`).

**`IcsDateValue`:**
- Purpose: Models the four ICS date forms explicitly so that timezone handling is not lost.
- Examples: `core/src/commonMain/kotlin/at/zanko/kalenderwecker/ics/models/IcsDateValue.kt`
- Pattern: `sealed interface` with `Date`, `Floating`, `Zoned`, `Utc` variants. Extension `toInstant()` is exhaustive over the variants.

**`CalendarParser`:**
- Purpose: Intended seam for swapping parser implementations.
- Examples: `core/src/commonMain/kotlin/at/zanko/kalenderwecker/ics/CalendarParser.kt`
- Pattern: `fun interface CalendarParser { fun parse(ics: String): List<CalendarEvent> }`.
- Status: Not implemented by anything yet. `BasicIcsParser.kt` exposes a top-level `parse` function instead of a class implementing this interface.

**`Platform` / `getPlatform()`:**
- Purpose: Example of the expect/actual seam for platform-specific info.
- Examples: `app/shared/src/commonMain/kotlin/at/zanko/kalenderwecker/Platform.kt`
- Pattern: `interface Platform { val name: String }` plus `expect fun getPlatform(): Platform`. Actuals exist for each target.

**`Property` (private):**
- Purpose: Pairs a raw ICS property value with its parameters (`TZID`, `VALUE`).
- Examples: `core/src/commonMain/kotlin/at/zanko/kalenderwecker/ics/BasicIcsParser.kt`
- Pattern: private `data class` scoped to the parser file.

## Entry Points

**Desktop:**
- Location: `app/desktopApp/src/main/kotlin/at/zanko/kalenderwecker/main.kt`
- Triggers: `./gradlew :app:desktopApp:run` (main class `at.zanko.kalenderwecker.MainKt`).
- Responsibilities: Opens a 'KalenderWecker' window and renders `App()`.

**Android:**
- Location: `app/androidApp/src/main/kotlin/at/zanko/kalenderwecker/MainActivity.kt`
- Triggers: Android launcher (`applicationId` `at.zanko.kalenderwecker`).
- Responsibilities: `enableEdgeToEdge()` and `setContent { App() }`. Also contains an `@Preview` composable.

**Web:**
- Location: `app/webApp/src/webMain/kotlin/at/zanko/kalenderwecker/main.kt`
- Triggers: `wasmJsBrowserDevelopmentRun` or `jsBrowserDevelopmentRun`.
- Responsibilities: Mounts `App()` with `ComposeViewport`.

**iOS:**
- Location: `app/iosApp/iosApp/iOSApp.swift` (`@main`), `app/iosApp/iosApp/ContentView.swift`, and `app/shared/src/iosMain/kotlin/at/zanko/kalenderwecker/MainViewController.kt`
- Triggers: Launch from Xcode.
- Responsibilities: Hosts the Compose UI through `ComposeUIViewController { App() }`.

**Server:**
- Location: `server/src/main/kotlin/at/zanko/kalenderwecker/Application.kt`
- Triggers: `./gradlew :server:run` (main class `at.zanko.kalenderwecker.ApplicationKt`).
- Responsibilities: Serves `GET /` on port 8080.

## Architectural Constraints

- **Threading:** The core parser is synchronous and single-threaded, with no coroutines. The server uses Ktor's Netty event loop. The desktop, web, and Compose UI run on their default UI dispatchers. No explicit threading model exists yet.
- **Global state:** No module-level mutable singletons were found in `core` or `app/shared`. `Greeting` and `App` create their own state per composition.
- **Circular imports:** None observed. Dependency direction is strictly `clients -> app/shared -> core` and `server -> core`.
- **Package layout:** All modules share the package `at.zanko.kalenderwecker`. Platform-specific source sets reuse it. Keep this in mind when adding code, because split-package collisions are possible with the same class names across `commonMain` and platform source sets.
- **Target coverage:** `core` builds for iOS, JVM, JS, Wasm, and Android. `server` is JVM-only, so it cannot consume `app/shared` UI code.
- **Hardcoded configuration:** Server port `8080` and host `0.0.0.0` are literals in `Application.kt`. `server/src/test/kotlin/at/zanko/kalenderwecker/PortConfigTest.kt` documents this as a known gap.

## Anti-Patterns

### Top-level `parse` instead of the `CalendarParser` seam

**What happens:** `core/src/commonMain/kotlin/at/zanko/kalenderwecker/ics/BasicIcsParser.kt` defines a top-level `fun parse(ics: String)`. `CalendarParser` in `ics/CalendarParser.kt` is never implemented or referenced.
**Why it's wrong:** Callers cannot inject alternative parsers or fakes through the declared interface, and the interface looks used when it is not.
**Do this instead:** Either implement `CalendarParser` in an object or class (for example `object BasicIcsParser : CalendarParser`) and depend on the interface from consumers, or delete the unused interface.

### Dead private helpers in the parser

**What happens:** `isDate()` and `normalizeDate()` in `BasicIcsParser.kt` are private and never called. Date handling goes through `parseIcsDate()` instead.
**Why it's wrong:** Two date parsers with different rules (`normalizeDate` also accepts a `T`-form without seconds) drift apart.
**Do this instead:** Keep a single date path through `parseIcsDate()`, and remove the unused helpers when touching that file.

### Empty placeholder file

**What happens:** `core/src/commonMain/kotlin/at/zanko/kalenderwecker/ics/DataHandler.kt` contains only a `package` line.
**Why it's wrong:** It signals a layer (data loading or caching) that does not exist yet. Future work may place fetch or storage logic here by accident.
**Do this instead:** Add the data layer deliberately, for example as an interface in `core` and an implementation in `app/shared` or `server`, and avoid putting platform I/O into `core`.

### Template UI in the shared module

**What happens:** `App.kt` is the Compose sample. `Greeting.kt` and `Platform.kt` are scaffold code that the calendar feature does not use.
**Why it's wrong:** New UI work may be built on top of template scaffolding and inherit its shape.
**Do this instead:** Introduce feature composables in new files under `app/shared/src/commonMain/kotlin/at/zanko/kalenderwecker/` and route `App()` to them, rather than extending the sample button.

## Error Handling

**Strategy:** Fail-fast with Kotlin `require` / `error` in the core parser. Invalid input throws `IllegalArgumentException` or `IllegalStateException`. No `Result` type is used.

**Patterns:**
- Structural checks in `parse()`: `require("BEGIN:VCALENDAR" in lines)` and `require("END:VCALENDAR" in lines)`.
- Per-event validation in `createEvent()`: `error("Event without UID")`, `error("Event without DTSTART")`.
- Malformed date strings throw from `parseIcsDate()` (`require` or `error`). A single bad event therefore fails the whole `parse()` call. There is no per-event recovery.
- `toIntOrNull()` is used for `SEQUENCE`, so a malformed sequence falls back to `0` silently.

## Cross-Cutting Concerns

**Logging:** Not implemented. Logback is a server dependency (`server/build.gradle.kts`) but no logger is used in source.
**Validation:** Inline in the parser (`require`, `Regex` matching). No shared validation layer.
**Authentication:** Not implemented. No auth code, tokens, or secrets handling exists in the repo.
**Dependency injection:** Declared in `gradle/libs.versions.toml` (Koin 4.2.2 entries) but not used by any module's build file or source.
**Serialization/networking:** Ktor client is referenced only for tests (`ktor-serverTestHost`). No HTTP client for fetching ICS feeds is wired.

---

*Architecture analysis: 2026-11-27*
