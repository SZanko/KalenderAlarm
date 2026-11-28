---
last_mapped_commit: 178eeade16d92a0598e3d1178742d6bf8e72edfd
last_mapped_at: 2026-11-27
---
# Coding Conventions

**Analysis Date:** 2026-11-27

## Naming Patterns

**Files:**
- Kotlin source files are PascalCase named after their primary type or purpose: `core/src/commonMain/kotlin/at/zanko/kalenderwecker/ics/BasicIcsParser.kt`, `core/src/commonMain/kotlin/at/zanko/kalenderwecker/ics/CalendarParser.kt`, `core/src/commonMain/kotlin/at/zanko/kalenderwecker/ics/models/IcsDateValue.kt`
- Platform `actual` files use the `<Name>.<platform>.kt` suffix: `app/shared/src/jvmMain/kotlin/at/zanko/kalenderwecker/Platform.jvm.kt`, `app/shared/src/androidMain/kotlin/at/zanko/kalenderwecker/Platform.android.kt`, `app/shared/src/iosMain/kotlin/at/zanko/kalenderwecker/Platform.ios.kt`, `app/shared/src/wasmJsMain/kotlin/at/zanko/kalenderwecker/Platform.wasmJs.kt`
- Test files are named `<Subject>Test.kt`: `core/src/commonTest/kotlin/at/zanko/kalenderwecker/BasicIcsParserTest.kt`, `server/src/test/kotlin/at/zanko/kalenderwecker/ApplicationTest.kt`
- Entry points are lowercase `main.kt` (`app/desktopApp/src/main/kotlin/at/zanko/kalenderwecker/main.kt`, `app/webApp/src/webMain/kotlin/at/zanko/kalenderwecker/main.kt`) or `Application.kt` on the server (`server/src/main/kotlin/at/zanko/kalenderwecker/Application.kt`)
- `core/src/commonMain/kotlin/at/zanko/kalenderwecker/ics/DataHandler.kt` is an empty file (package line only). Do not treat it as a pattern to copy.

**Packages:**
- Everything lives under `at.zanko.kalenderwecker`. Subpackages mirror directories: `at.zanko.kalenderwecker.ics`, `at.zanko.kalenderwecker.ics.models`.
- Package declarations match the directory path exactly.

**Functions:**
- camelCase: `parse`, `unfold`, `parseProperty`, `parseIcsDate`, `createEvent`, `normalizeDate`, `sayHello`, `getPlatform`
- Private helpers are top-level `private fun` in the same file rather than members of a class: see the helpers at the bottom of `core/src/commonMain/kotlin/at/zanko/kalenderwecker/ics/BasicIcsParser.kt`
- Compose functions are PascalCase and annotated `@Composable`: `App()` in `app/shared/src/commonMain/kotlin/at/zanko/kalenderwecker/App.kt`, `InfiniteScrollList(...)` in `app/shared/src/commonMain/kotlin/at/zanko/kalenderwecker/InfiniteScrollList.kt`
- Test functions use one of two styles, and both exist:
  - Backtick names with spaces: `` fun `example data exists`() `` in `core/src/commonTest/kotlin/at/zanko/kalenderwecker/BasicIcsParserTest.kt`
  - Underscore GIVEN/WHEN/THEN names: `gIVEN_validIcsFileAsString_WHEN_parse_THEN_returnEventList` in the same file (the leading `g` is lowercase, which is a typo, not a convention)
  - Prefer the backtick style for new tests. It is the more common Kotlin idiom and reads better in test reports.

**Variables and properties:**
- camelCase: `insideEvent`, `recurrenceIdParsed`, `floatingTimeZone`, `dateTimeString`
- Immutable by default. Use `val`. `var` appears only for loop-scoped or local mutable state: `var insideEvent = false`, `var showContent by remember { mutableStateOf(false) }`
- Mutable collections are declared as `mutableListOf` / `mutableMapOf` locally and exposed as `List` (`return events`)
- Map keys from ICS headers are normalized to uppercase: `name = header.first().uppercase()`, `params` keys are uppercased in `parseProperty`. Always look up ICS property names in uppercase (`p["DTSTART"]`, `property.params["TZID"]`).

**Types:**
- PascalCase classes and interfaces: `CalendarEvent`, `IcsDateValue`, `CalendarParser`, `Platform`, `JVMPlatform`
- Nested sealed variants inside the sealed interface, named by the ICS concept: `IcsDateValue.Date`, `IcsDateValue.Floating`, `IcsDateValue.Zoned`, `IcsDateValue.Utc` (`core/src/commonMain/kotlin/at/zanko/kalenderwecker/ics/models/IcsDateValue.kt`)
- Private data classes for internal parsing state: `private data class Property(val value: String, val params: Map<String, String>)` in `BasicIcsParser.kt`
- Exception to the naming rule: `JVMPlatform` (acronym uppercase) versus `Greeting` / `Platform` elsewhere. Keep acronyms uppercase only when they are the class name prefix.

**Constants:**
- No top-level `const val` declarations were found in the repository. If you add constants, use `UPPER_SNAKE_CASE` in a companion object or top-level `private const val`.

## Code Style

**Formatting:**
- Official Kotlin style is declared in `gradle.properties` via `kotlin.code.style=official`. Follow it.
- Indentation is 4 spaces. Continuation lines in expressions use 8 spaces (see `parseIcsDate` in `BasicIcsParser.kt`).
- No formatter is wired into the build. Formatting drift exists, for example `start =parseIcsDate(` (missing space) in `createEvent` in `core/src/commonMain/kotlin/at/zanko/kalenderwecker/ics/BasicIcsParser.kt`, and `JVMPlatform: Platform` (missing space) in `app/shared/src/jvmMain/kotlin/at/zanko/kalenderwecker/Platform.jvm.kt`. Do not copy these.
- Trailing newlines are inconsistent. Several files (`CalendarParser.kt`, `CalendarEvent.kt`, `IcsDateValue.kt`, `GreetingUtil.kt`) lack a final newline. New files should end with one.
- Trailing commas are used in multi-line parameter lists in the Compose and model code (the `CalendarEvent` constructor in `core/src/commonMain/kotlin/at/zanko/kalenderwecker/ics/models/CalendarEvent.kt` has none, while the Compose `Column` call in `App.kt` does). Pick trailing commas for any new multi-line parameter list.
- Blank lines separate logical sections inside functions (`unfold`, `parseIcsDate`), with a blank line before `return`s that follow a block.

**Linting:**
- No ktlint, detekt, or Spotless configuration exists in the repository (`build.gradle.kts`, `settings.gradle.kts`, `gradle/libs.versions.toml` contain none).
- The only IDE inspection profile is `.idea/inspectionProfiles/Project_Default.xml`, which enables only Compose multiplatform preview checks (`MultiplatformPreviewAnnotationInFunctionWithParameters`, `MultiplatformPreviewMultipleParameterProviders`).
- Enforce style by reading neighboring code, not by relying on tools.

## Import Organization

**Order (observed):**
1. Project imports (`at.zanko.kalenderwecker.*`)
2. Library imports (`kotlinx.datetime.*`, `io.ktor.*`, `androidx.compose.*`, `com.goncalossilva.resources.*`, `okio.*`)
3. Standard library imports (`kotlin.*`, `kotlin.time.*`, `kotlin.test.*`) are usually last, but the order is not enforced. Alphabetical ordering is the closest rule. Example: `core/src/commonMain/kotlin/at/zanko/kalenderwecker/ics/BasicIcsParser.kt`

**Wildcard vs explicit:**
- Explicit imports are the default: `import kotlinx.datetime.LocalDate` (`BasicIcsParser.kt`)
- Wildcard imports are used in server and Ktor test code: `import io.ktor.server.application.*`, `import kotlin.test.*` (`server/src/main/kotlin/at/zanko/kalenderwecker/Application.kt`, `server/src/test/kotlin/at/zanko/kalenderwecker/ApplicationTest.kt`). Keep wildcards to Ktor DSL and `kotlin.test` only.

**Path Aliases:**
- None. Kotlin Multiplatform packages are referenced by full package name.

**Multiplatform dependency exposure:**
- `app/shared/build.gradle.kts` exposes the core module with `api(project(":core"))`. Platform apps depend on `:app:shared` and get core transitively. Do not add `core` dependencies directly to platform app modules.

## Error Handling

**Strategy:** Fail fast on malformed input with Kotlin's standard precondition functions. Tolerate unknown or optional content silently.

**Patterns:**
- Argument and format validation uses `require(condition) { "message" }`. Example in `core/src/commonMain/kotlin/at/zanko/kalenderwecker/ics/BasicIcsParser.kt`:
  ```kotlin
  require("BEGIN:VCALENDAR" in lines) {
      "Invalid ICS: missing VCALENDAR header"
  }
  ```
- Invalid state and missing required data uses `error(...)` or the elvis form `?: error("...")`:
  ```kotlin
  val uid = p["UID"]?.value
      ?: error("Event without UID")
  ```
- Regex format checks use `matchEntire(...) ?: error("Invalid ICS DATE-TIME: $raw")` in `parseIcsDate`.
- Optional values use safe calls and defaults, not exceptions: `p["SUMMARY"]?.value?.let(::unescape) ?: "Untitled"`, `p["SEQUENCE"]?.value?.toIntOrNull() ?: 0`.
- Unparseable lines are skipped without error. `parseProperty` returns `null` when a line has no `:` and the caller ignores it (`if (parsed != null)`).
- Error messages include the offending input value (`"Invalid ICS DATE: $raw"`, `"UTC DATE-TIME must not contain TZID: $raw"`). Keep this pattern for new validation.
- Message casing is inconsistent: `"Event without UID"` and `"Unsupported ICS date: $value"` both appear. Start new messages with a capital letter.
- No custom exception types exist. Use `IllegalArgumentException` (via `require`) or `IllegalStateException` (via `error`).
- The server has no error handling. `Application.kt` has one route that cannot fail.

## Logging

**Framework:** None in application code. No `println`, `Logger`, or `LoggerFactory` calls exist in any Kotlin source.

**Configuration present:**
- `logback-classic` is a dependency of the server (`server/build.gradle.kts`, `implementation(libs.logback)`).
- `server/src/main/resources/logback.xml` sets the root level to `trace` and silences `org.eclipse.jetty` and `io.netty` to `INFO`. Expect verbose output when logging is added. Lower the root level before shipping.

**Patterns:**
- When adding logging to the server, obtain loggers through SLF4J (provided by logback) and keep shared `core` code free of JVM-only logging, because `core` targets JS, Wasm, iOS, and Android as well as JVM.

## Comments

**When to Comment:**
- Document ICS format rules inline with a short line comment above the branch that handles them. Examples in `BasicIcsParser.kt`: `// DATE: yyyyMMdd`, `// DATE-TIME: yyyyMMddTHHmmss[Z]`, and in `IcsDateValue.kt`: `// VALUE=DATE`, `// DATE-TIME without TZID or Z`
- Comment the reason for a non-obvious data decision, not the mechanics.
- Test files may use a block comment to explain a known gap, as in `server/src/test/kotlin/at/zanko/kalenderwecker/PortConfigTest.kt`.

**JSDoc/KDoc:**
- KDoc is not used on public API. `CalendarParser`, `parse`, and `CalendarEvent` have no doc comments.
- For any new public function in `core`, add a KDoc comment stating the accepted input format and what the exception types mean.

## Function Design

**Size:**
- Keep helpers small and single-purpose, as in `unescape`, `unfold`, `normalizeDate`. The main parser loop `parse` is the largest function at about 40 lines.
- Long nested logic is split into private helpers in the same file.

**Parameters:**
- Use named arguments when constructing data classes with more than two fields: `IcsDateValue.Zoned(value = dateTime, tzid = tzid)`
- Pass lambdas with trailing syntax for Compose: `InfiniteScrollList(itemCount, loadMoreItems) { index -> ... }`
- Prefer function references for transformations: `?.let(::unescape)`, `?.let(::parseIcsDate)`

**Return Values:**
- Use expression bodies for single-expression functions: `fun sayHello(to: String): String = "Hello, $to!"` (`core/src/commonMain/kotlin/at/zanko/kalenderwecker/GreetingUtil.kt`)
- Use block bodies with explicit `return` for functions that have control flow (`parseIcsDate`, `unfold`)
- Return nullable types (`IcsDateValue?`, `Pair<String, Property>?`) to signal "not applicable" rather than throwing
- Return `List<T>` from parsers, not `MutableList<T>`

**Modeling:**
- Use `data class` for value types (`CalendarEvent`, `Property`)
- Use `sealed interface` with `data class` variants when a value has several shapes (`IcsDateValue`)
- Use `fun interface` for single-method abstractions (`CalendarParser` in `core/src/commonMain/kotlin/at/zanko/kalenderwecker/ics/CalendarParser.kt`)
- Use `expect`/`actual` for platform-specific values (`Platform.kt` in `app/shared/src/commonMain/kotlin/at/zanko/kalenderwecker/`)

## Module Design

**Exports:**
- Kotlin visibility is the export mechanism. Expose parsers and models as public top-level functions and classes. Mark internals `private` at file level.

**Barrel Files:**
- No barrel files. Import each type from its own package, for example `at.zanko.kalenderwecker.ics.models.CalendarEvent`.

**Compose and platform code:**
- Shared UI lives in `app/shared/src/commonMain/kotlin/at/zanko/kalenderwecker/`. Platform entry points are thin wrappers that call `App()`: `app/desktopApp/src/main/kotlin/at/zanko/kalenderwecker/main.kt`, `app/webApp/src/webMain/kotlin/at/zanko/kalenderwecker/main.kt`, `app/androidApp/src/main/kotlin/at/zanko/kalenderwecker/MainActivity.kt`
- Compose previews use `@Preview` on `@Composable` functions (`App.kt`, `MainActivity.kt`)

---

*Convention analysis: 2026-11-27*
