---
last_mapped_commit: 178eeade16d92a0598e3d1178742d6bf8e72edfd
last_mapped_at: 2026-11-27
---
# Testing Patterns

**Analysis Date:** 2026-11-27

## Test Framework

**Runner:**
- Kotlin Test (`kotlin.test`), version pinned via the catalog entry `kotlin-test` (`gradle/libs.versions.toml`, Kotlin `2.4.21`)
- JUnit 4 runner on the JVM via `kotlin-test-junit` (`server/build.gradle.kts`), JUnit `4.13.2` in the catalog
- Multiplatform test source sets follow the Kotlin Gradle convention. Configs are in `core/build.gradle.kts` and `app/shared/build.gradle.kts`, not in standalone test config files
- No `jest.config`, `vitest.config`, or `kotest` config exists

**Assertion Library:**
- `kotlin.test` assertions only: `assertEquals`, `assertTrue` (`core/src/commonTest/kotlin/at/zanko/kalenderwecker/BasicIcsParserTest.kt`)

**Test resource loading:**
- `com.goncalossilva:resources` (catalog key `kotlinx-resources`, plugin `kotlinx-resources`) loads files from `commonTest/resources` on every target. Use `Resource("file.ics").readText()` and `Resource(...).exists()` as in `BasicIcsParserTest.kt`.

**Run Commands:**
```bash
./gradlew :core:allTests                 # Run all core multiplatform tests
./gradlew :core:jvmTest                  # Core tests on the JVM only (fastest)
./gradlew :core:jsTest                   # Core tests on JS (karma; see core/karma.config.d/)
./gradlew :core:wasmJsTest               # Core tests on Wasm
./gradlew :core:testAndroidHostTest      # Core tests on the Android host
./gradlew :app:shared:jvmTest            # Shared app tests on the JVM
./gradlew :app:shared:testAndroidHostTest
./gradlew :app:shared:wasmJsTest
./gradlew :app:shared:jsTest
./gradlew :app:shared:iosSimulatorArm64Test
./gradlew :server:test                   # Ktor server tests (JVM)
```
- The `README.md` lists the app-shared variants. The `:core` commands above are derived from the `core/build.gradle.kts` targets and are not documented in the README.
- Watch mode and coverage commands are not configured. No `kover` or `jacoco` plugin exists.

## Test File Organization

**Location:**
- Core shared logic tests: `core/src/commonTest/kotlin/at/zanko/kalenderwecker/` (run on every core target)
- Core test data: `core/src/commonTest/resources/` (three `.ics` fixtures, see Fixtures below)
- App shared tests are split per platform source set:
  - `app/shared/src/commonTest/kotlin/at/zanko/kalenderwecker/SharedCommonTest.kt`
  - `app/shared/src/jvmTest/kotlin/at/zanko/kalenderwecker/SharedLogicDesktopTest.kt`
  - `app/shared/src/webTest/kotlin/at/zanko/kalenderwecker/SharedLogicWebTest.kt`
  - `app/shared/src/androidHostTest/kotlin/at/zanko/kalenderwecker/SharedLogicAndroidHostTest.kt`
  - `app/shared/src/iosTest/kotlin/at/zanko/kalenderwecker/SharedLogicIOSTest.kt`
- Server tests are JVM-only: `server/src/test/kotlin/at/zanko/kalenderwecker/ApplicationTest.kt`, `server/src/test/kotlin/at/zanko/kalenderwecker/PortConfigTest.kt`

**Naming:**
- Test classes: `<Subject>Test` (`BasicIcsParserTest`, `ApplicationTest`, `PortConfigTest`)
- Test functions: see Naming Patterns in `CONVENTIONS.md`. Prefer backtick names.
- Platform source sets in `app/shared` keep the scaffold names `SharedLogic<Platform>Test`. Those are placeholders (see Test Types).

**Structure:**
```
core/src/commonTest/
├── kotlin/at/zanko/kalenderwecker/
│   ├── BasicIcsParserTest.kt      # Real parser tests
│   └── TestUtils.kt               # Empty stub, imports okio.FileSystem only
└── resources/
    ├── TimeEdit_UltraShort.ics    # 41 lines, used for event count
    ├── TimeEdit_Short.ics         # 623 lines, existence check only
    └── TimeEdit_Software_Engineering_for_AI_master__TASEF-26H-ML026_2026-11-04_10_27.ics   # 487 lines, not referenced yet
```
- Test package names match the production package, so tests can access `internal` and `private`-at-package members if needed. Production code currently imports public API explicitly, as `BasicIcsParserTest.kt` does with `import at.zanko.kalenderwecker.ics.parse`.

## Test Structure

**Suite Organization:**
```kotlin
package at.zanko.kalenderwecker

import at.zanko.kalenderwecker.ics.parse
import kotlin.test.Test
import kotlin.test.assertTrue
import com.goncalossilva.resources.Resource
import kotlin.test.assertEquals

class BasicIcsParserTest {
    @Test
    fun `example data exists`() {
        val resource = Resource("TimeEdit_Short.ics")
        assertTrue(resource.exists())
    }

    @Test
    fun gIVEN_validIcsFileAsString_WHEN_parse_THEN_returnEventList() {
        val ics = Resource("TimeEdit_UltraShort.ics")
        assertTrue(ics.exists())

        val result = parse(ics.readText())
        assertEquals(3, result.size)
    }
}
```
- One test class per production file, one `@Test` per behavior.
- No `@BeforeTest`, `@AfterTest`, or `@BeforeEach` setup exists yet. Add `@BeforeTest` (kotlin.test) for shared setup, not a base class.
- Each test creates its own input inline or loads a resource. No shared mutable state.

**Patterns:**
- Setup: load fixtures inline with `Resource(...)`. There is no setup method.
- Teardown: none needed. Tests have no side effects.
- Assertion: prefer `assertEquals(expected, actual)` (expected first). Use `assertTrue` for existence checks.
- A `require` or `error` path should be tested with `assertFailsWith<IllegalArgumentException> { ... }` (kotlin.test). No such test exists yet. Add one for the `require` messages in `BasicIcsParser.kt` (missing `VCALENDAR`, missing `UID`, invalid `DATE`).

**Server test pattern (Ktor):**
```kotlin
@Test
fun testRoot() = testApplication {
    application {
        module()
    }
    val response = client.get("/")
    assertEquals(HttpStatusCode.OK, response.status)
    assertEquals("Hello, Ktor!", response.bodyAsText())
}
```
- Use Ktor's `testApplication { application { module() } }` from `io.ktor.server.testing.*`. Do not start the Netty server in tests.
- The `module()` extension from `server/src/main/kotlin/at/zanko/kalenderwecker/Application.kt` is the testable entry point. Keep route setup inside `Application.module()`, not in `main()`.

## Mocking

**Framework:** None in use. MockK (`mockk`, `mockk-bdd`, `mockk-android` in `gradle/libs.versions.toml`, version `1.14.11`) is declared in the catalog but is not referenced by any `build.gradle.kts` or any test source. Adding it requires adding `testImplementation(libs.mockk)` to the relevant module.

**Patterns:**
- No mocks are present. Prefer real objects and pure functions. The parser is pure (`String` in, `List<CalendarEvent>` out), so tests need no mocks.
- If a platform dependency must be replaced, introduce an interface and a `fun interface` implementation, following `CalendarParser` in `core/src/commonMain/kotlin/at/zanko/kalenderwecker/ics/CalendarParser.kt`. Fakes are preferred over MockK for this codebase's small size.

**What to Mock:**
- External I/O boundaries only (network, file system, time). None exist yet.
- Platform-specific `expect` functions, such as `getPlatform()` from `app/shared/src/commonMain/kotlin/at/zanko/kalenderwecker/Platform.kt`, can be replaced with a fake per target.

**What NOT to Mock:**
- Pure parsing and model logic in `core/src/commonMain/kotlin/at/zanko/kalenderwecker/ics/`
- Kotlin data classes and sealed hierarchies such as `IcsDateValue`
- The Ktor routing pipeline. Use `testApplication` instead.

## Fixtures and Factories

**Test Data:**
- `.ics` fixtures live in `core/src/commonTest/resources/`. They are real TimeEdit exports, including `X-WR-CALNAME`, `PRODID` with escaped commas, folded lines (leading space continuation), and `DTSTART` in UTC form (`20261102T071500Z`). Keep these fixtures as realistic examples.
- `TimeEdit_UltraShort.ics` is the smallest parsing fixture (three events). Use it for fast unit tests.
- The inline pattern for small cases is a raw string or a `listOf(...).joinToString("\n")` block, which is not yet used. Add one per new ICS feature, for example a `VALUE=DATE` all-day event, a `TZID` event, or a `RECURRENCE-ID` override.

**Location:**
- Fixtures: `core/src/commonTest/resources/`
- Factories: none. `TestUtils.kt` in `core/src/commonTest/kotlin/at/zanko/kalenderwecker/` is a stub that imports `okio.FileSystem` and `okio.Path.Companion.toPath` but defines nothing. Put shared helpers there when they are needed.

## Coverage

**Requirements:** None enforced. No coverage plugin, threshold, or CI gate exists.

**View Coverage:**
```bash
# Not configured. To measure JVM coverage, add the Kover plugin to core/build.gradle.kts, then run:
./gradlew :core:koverHtmlReport
```

## Test Types

**Unit Tests:**
- The only real tests are in `core/src/commonTest/kotlin/at/zanko/kalenderwecker/BasicIcsParserTest.kt`. They cover file presence and a three-event parse count.
- The `BasicIcsParserTest` count assertion checks only the size of the result, not the content of each `CalendarEvent`. Add assertions on `uid`, `start`, `end`, `summary`, and `eventKey` when extending coverage.
- Scaffold placeholders in `app/shared` and the Ktor server only assert arithmetic (`assertEquals(3, 1 + 2)`). They verify the toolchain runs, not behavior. Replace them when real logic lands in those modules.

**Integration Tests:**
- `ApplicationTest.kt` (`server/src/test/kotlin/at/zanko/kalenderwecker/ApplicationTest.kt`) is the only integration test. It exercises the Ktor route through `testApplication` and checks status and body.

**E2E Tests:**
- Not used. No browser, device, or UI test framework is configured. Compose UI tests would need `compose-ui-test` added to `gradle/libs.versions.toml`.

**Untracked test file:**
- `server/src/test/kotlin/at/zanko/kalenderwecker/PortConfigTest.kt` is not yet committed (it appears in `git status` as untracked). Its own doc comment says the port is hardcoded to `8080` in `Application.kt` and that the tests only verify the root route, not the port. Do not treat its name as proof that port configuration exists. The file's assertions check `GET /` only.

## Common Patterns

**Async Testing:**
- No coroutine or suspending tests exist. When needed, use `kotlinx.coroutines.test.runTest` from the `kotlinx-coroutines` catalog entry (`1.11.0`), which is declared but not yet a test dependency in any module. Ktor's `testApplication` is already suspending-aware and accepts `suspend` assertions directly.

**Error Testing:**
- Not yet written. Use kotlin.test's `assertFailsWith` for the `require` and `error` paths in the parser:
```kotlin
@Test
fun `parse rejects input without VCALENDAR header`() {
    assertFailsWith<IllegalArgumentException> {
        parse("BEGIN:VEVENT\nEND:VEVENT")
    }
}
```
- Test that an event missing `UID` or `DTSTART` fails with `IllegalStateException` (thrown by `error(...)`), which is a different type from the `require` failures. Assert the type explicitly.

**Multiplatform test running:**
- The same `commonTest` code runs on JVM, JS, Wasm, iOS, and Android host. Avoid platform-only APIs in `core` tests. The `okio` file system APIs are used in `TestUtils.kt` for this reason.

---

*Testing analysis: 2026-11-27*
