---
last_mapped_commit: 178eeade16d92a0598e3d1178742d6bf8e72edfd
last_mapped_at: 2026-11-27
---
# Codebase Concerns

**Analysis Date:** 2026-11-27

## Tech Debt

**Stub and placeholder code shipped as if it were real:**
- Issue: `CalendarParser` is declared as a `fun interface` but nothing implements it. The file `core/src/commonMain/kotlin/at/zanko/kalenderwecker/ics/BasicIcsParser.kt` is named after a class but contains only top-level functions (`parse`) and no `BasicIcsParser` type. `DataHandler.kt` contains only a package line. The UI in `app/shared/src/commonMain/kotlin/at/zanko/kalenderwecker/App.kt` is still the Compose Multiplatform template ("Click me!", a Compose logo, `Greeting`).
- Files: `core/src/commonMain/kotlin/at/zanko/kalenderwecker/ics/CalendarParser.kt`, `core/src/commonMain/kotlin/at/zanko/kalenderwecker/ics/BasicIcsParser.kt`, `core/src/commonMain/kotlin/at/zanko/kalenderwecker/ics/DataHandler.kt`, `app/shared/src/commonMain/kotlin/at/zanko/kalenderwecker/App.kt`, `app/shared/src/commonMain/kotlin/at/zanko/kalenderwecker/Greeting.kt`, `core/src/commonMain/kotlin/at/zanko/kalenderwecker/GreetingUtil.kt`
- Impact: The parser cannot be injected or swapped through its interface, so every consumer is coupled to the top-level `parse` function. Template code will be mistaken for product code by planners.
- Fix approach: Either implement `CalendarParser` in a `BasicIcsParser` class (and rename the file to match) or delete the interface until it is needed. Remove `DataHandler.kt` or give it a purpose. Replace the template `App()` when the calendar UI starts.

**Placeholder tests that assert nothing meaningful:**
- Issue: Five platform test source sets contain only `assertEquals(3, 1 + 2)`.
- Files: `app/shared/src/commonTest/kotlin/at/zanko/kalenderwecker/SharedCommonTest.kt`, `app/shared/src/jvmTest/kotlin/at/zanko/kalenderwecker/SharedLogicDesktopTest.kt`, `app/shared/src/webTest/kotlin/at/zanko/kalenderwecker/SharedLogicWebTest.kt`, `app/shared/src/iosTest/kotlin/at/zanko/kalenderwecker/SharedLogicIOSTest.kt`, `app/shared/src/androidHostTest/kotlin/at/zanko/kalenderwecker/SharedLogicAndroidHostTest.kt`
- Impact: Green test runs for `:app:shared` on every target say nothing about the app. The numbers reported by `./gradlew :app:shared:jvmTest` and similar commands are misleading.
- Fix approach: Delete the placeholders or replace them with tests of real shared logic as it lands.

**Empty test helper:**
- Issue: `TestUtils.kt` has only a package line and imports of `okio.FileSystem` and `okio.Path.Companion.toPath`. Nothing uses them, so `okio` is effectively unused.
- Files: `core/src/commonTest/kotlin/at/zanko/kalenderwecker/TestUtils.kt`, `gradle/libs.versions.toml` (okio entry)
- Impact: Dead dependency on every platform. Misleads readers into thinking shared test helpers exist.
- Fix approach: Remove the file and the okio dependency, or write the helpers the file was meant to hold.

**Dead and duplicated code in the ICS parser:**
- Issue: `isDate` and `normalizeDate` in `BasicIcsParser.kt` are never called. `normalizeDate` contains HHmm-time handling that no code path reaches. `createEvent` parses `DTSTART` twice (once as the `start` value through the `?: error(...)` branch and again through `p["DTSTART"] ?: error("Missing DTSTART")`), and the earlier `val start = p["DTSTART"]` is only used for its null check.
- Files: `core/src/commonMain/kotlin/at/zanko/kalenderwecker/ics/BasicIcsParser.kt`
- Impact: Two parallel date-parsing paths (`parseIcsDate` and `normalizeDate`) can drift. Readers cannot tell which one is authoritative.
- Fix approach: Delete `isDate` and `normalizeDate`. Parse `DTSTART` once and reuse the value. If HHmm support is wanted, add it to `parseIcsDate` and test it.

**Unused and unfinished dependency entries:**
- Issue: `gradle/libs.versions.toml` declares Koin (`koin-core`, `koin-compose`, `koin-compose-viewmodel`, `koin-android`, `koin-android-compose`, `koin-android-compose-viewmodel`), `mockk`, `mockk-bdd`, `mockk-android`, and `androidx-espresso-core` entries that no build file references. `build.gradle.kts` files reference `libs.logback`, `libs.ktor.*`, `libs.kotlinx.resources`, and similar entries that are in use.
- Files: `gradle/libs.versions.toml`, `app/shared/build.gradle.kts`, `core/build.gradle.kts`
- Impact: Version bumps and security scanning cover libraries that are not on the classpath. The catalog grows out of sync with reality.
- Fix approach: Remove unreferenced catalog entries and add them back when a feature needs them.

**Toolchain and JDK versions disagree:**
- Issue: `flake.nix` sets `javaVersion = 25`. `gradle/gradle-daemon-jvm.properties` sets `toolchainVersion=21` with an Azul vendor. Several modules set `jvmTarget = JVM_11` (`core/build.gradle.kts`, `app/shared/build.gradle.kts`). Gradle is pinned to 9.8.2 in `gradle/wrapper/gradle-wrapper.properties`.
- Files: `flake.nix`, `gradle/gradle-daemon-jvm.properties`, `core/build.gradle.kts`, `app/shared/build.gradle.kts`, `gradle/wrapper/gradle-wrapper.properties`
- Impact: Developers on the Nix shell and on a Gradle toolchain-resolving machine may build against different JDKs. The JVM_11 target is well below the toolchain and is a source of subtle bytecode and API mismatches.
- Fix approach: Pick one JDK version for the Nix shell, the toolchain, and the Kotlin `jvmTarget`, and state it in one place.

**Generated artifacts committed to git:**
- Issue: `kotlin-js-store/package-lock.json`, `kotlin-js-store/yarn.lock`, and `kotlin-js-store/wasm/yarn.lock` are tracked. `kotlin-js-store/` is not in `.gitignore`.
- Files: `kotlin-js-store/` (tracked files listed above), `.gitignore`
- Impact: Lockfile churn appears in every Kotlin/JS build. Generated state is reviewed as if it were source.
- Fix approach: Add `kotlin-js-store/` to `.gitignore` and remove the tracked files with `git rm --cached`.

**Stray and uncommitted working-tree state:**
- Issue: `app/desktopApp/gradle/` (only a `wrapper/gradle-wrapper.properties` file, no wrapper jar or script) and `core/karma.config.d/` are untracked. The root contains `hs_err_pid1586.log` (a 442 KB JVM crash dump, hidden only by the `*.log` ignore rule). `BasicIcsParserTest.kt` and `.gitignore`, `flake.lock`, and `gradle/libs.versions.toml` have uncommitted edits.
- Files: `app/desktopApp/gradle/wrapper/gradle-wrapper.properties`, `core/karma.config.d/`, `hs_err_pid1586.log`, `core/src/commonTest/kotlin/at/zanko/kalenderwecker/BasicIcsParserTest.kt`
- Impact: A partial wrapper directory inside a module can confuse Gradle and IDEs about which wrapper is authoritative. Crash dumps are a sign of an unstable desktop run that has not been investigated.
- Fix approach: Remove the nested wrapper directory if it is not needed. Review and commit or revert the karma config. Read the crash log for the cause before deleting it.

**Server port test documents a missing feature:**
- Issue: `server/src/test/kotlin/at/zanko/kalenderwecker/PortConfigTest.kt` says port configurability (QUAL-03) "requires implementation change" and does not test the port at all. Its two tests duplicate `ApplicationTest.testRoot`. The main function in `server/src/main/kotlin/at/zanko/kalenderwecker/Application.kt` still hard-codes `port = 8080`.
- Files: `server/src/test/kotlin/at/zanko/kalenderwecker/PortConfigTest.kt`, `server/src/main/kotlin/at/zanko/kalenderwecker/Application.kt`
- Impact: The test suite reports a requirement as covered when it is not. Readers may think the port is configurable.
- Fix approach: Implement the environment lookup (`System.getenv("SERVER_PORT")?.toIntOrNull() ?: 8080`) and test it through a function that takes the port as a parameter, or remove the test until the feature exists.

**Private calendar URL committed to the repository:**
- Issue: `Stuff.md` (tracked) contains a full TimeEdit ICS subscription URL. The path segment is an unguessable identifier that works as a bearer credential for the user's timetable.
- Files: `Stuff.md`
- Impact: Anyone with repository access, or any clone or fork, can read the timetable. The URL persists in git history even if the file is later edited.
- Fix approach: Treat the URL as a secret. Remove it from `Stuff.md`, rotate or regenerate the TimeEdit link if the feed is public, and keep the URL in an untracked local file or environment variable. Consider rewriting history if the repository is shared.

**Personal fixtures committed as test data:**
- Issue: `core/src/commonTest/resources/` contains three real TimeEdit exports including course names, course codes, lecture titles, and UIDs on the `timeedit.com` domain.
- Files: `core/src/commonTest/resources/TimeEdit_Short.ics`, `core/src/commonTest/resources/TimeEdit_UltraShort.ics`, `core/src/commonTest/resources/TimeEdit_Software_Engineering_for_AI_master__TASEF-26H-ML026_2026-11-04_10_27.ics`
- Impact: Personal schedule data is versioned. The larger file is not referenced by any test.
- Fix approach: Replace the fixtures with synthetic events that exercise the same features (folding, escaping, DATE, TZID, UTC, multiple events), and keep real exports out of git.

## Known Bugs

**Escape sequences are unescaped in the wrong order:**
- Symptoms: Text containing an escaped backslash followed by `n` (for example the ICS text `C:\\new`) is rendered as `C:\` plus a newline plus `ew`. The `\\n` newline replacement runs before the `\\\\` (escaped backslash) replacement, so the backslash that belongs to the escaped pair is consumed.
- Files: `core/src/commonMain/kotlin/at/zanko/kalenderwecker/ics/BasicIcsParser.kt` (`unescape`)
- Trigger: Any `SUMMARY`, `DESCRIPTION`, or `LOCATION` containing `\\` followed by `n`, `N`, `,`, or `;`.
- Workaround: None. Fix by unescaping in a single left-to-right pass over the characters, handling `\\`, `\n`, `\N`, `\,`, and `\;` in one loop.

**Nested components leak properties into the event:**
- Symptoms: Properties of a nested `VALARM` (for example its `DESCRIPTION`, `ACTION`, or `TRIGGER`) are stored in the same flat `properties` map as the `VEVENT` properties. Because the map is keyed by name and the last write wins, an alarm `DESCRIPTION` replaces the event's own `DESCRIPTION`.
- Files: `core/src/commonMain/kotlin/at/zanko/kalenderwecker/ics/BasicIcsParser.kt` (`parse`, which handles only `BEGIN:VEVENT` and `END:VEVENT`)
- Trigger: Any feed whose events contain a `BEGIN:VALARM` block after the event's own description. The current fixtures contain no `VALARM`, so the bug is latent in tests.
- Workaround: None. Track component depth, and ignore properties while inside a nested component other than `VEVENT`.

**One malformed event aborts the whole feed:**
- Symptoms: `createEvent` calls `error(...)` for a missing `UID` or `DTSTART`, and `parseIcsDate` calls `require` or `error` for a bad date. The exception propagates out of `parse`, so no events from the feed are returned.
- Files: `core/src/commonMain/kotlin/at/zanko/kalenderwecker/ics/BasicIcsParser.kt` (`createEvent`, `parseIcsDate`, `parse`)
- Trigger: A single event with a missing `UID`, a missing `DTSTART`, or an unrecognized date format in any feed.
- Workaround: None. Catch exceptions per event in `parse`, skip or log the bad event, and return the rest.

**Header and footer checks are case-sensitive and whitespace-sensitive:**
- Symptoms: `"BEGIN:VCALENDAR" in lines` and `"END:VCALENDAR" in lines` compare exact strings. Event delimiters are matched case-insensitively through `line.uppercase()`. A feed whose header is lowercase, or whose last line has trailing spaces, is rejected with "Invalid ICS" or "Incomplete ICS feed" even though its events parse fine.
- Files: `core/src/commonMain/kotlin/at/zanko/kalenderwecker/ics/BasicIcsParser.kt` (`parse`)
- Trigger: Feeds produced by tools that add trailing whitespace or lowercase component names.
- Workaround: None. Apply the same normalization (trim and uppercase) to both the component checks and the delimiter checks.

**Recurring events are not expanded and duplicate keys are possible:**
- Symptoms: `RRULE` and `EXDATE` are ignored. A recurring series shows only its first occurrence. `eventKey` is built as `"$uid|${recurrenceId.orEmpty()}"`, so two `VEVENT` blocks that share a `UID` and have no `RECURRENCE-ID` produce identical keys. Any UI that uses `eventKey` as a list or map key will break on duplicates.
- Files: `core/src/commonMain/kotlin/at/zanko/kalenderwecker/ics/BasicIcsParser.kt` (`createEvent`), `core/src/commonMain/kotlin/at/zanko/kalenderwecker/ics/models/CalendarEvent.kt`
- Trigger: Any weekly or daily series in a feed, or any feed that duplicates a `UID`.
- Workaround: None. Document that the parser returns base events only, and add a uniqueness strategy (for example including `DTSTART` in the key) before using `eventKey` in the UI.

**Windows time zone names are not resolved:**
- Symptoms: `IcsDateValue.Zoned.toInstant` calls `TimeZone.of(tzid)`, which throws for names that are not IANA identifiers (for example Outlook's `W. Europe Standard Time`). The exception is raised late, when the instant is requested, not when the feed is parsed.
- Files: `core/src/commonMain/kotlin/at/zanko/kalenderwecker/ics/models/IcsDateValue.kt`
- Trigger: Any feed exported from Outlook or another Windows-based tool that uses Windows zone names.
- Workaround: None. Map known Windows zone names to IANA identifiers, and catch unknown ones explicitly.

**Floating times and all-day events have no instant:**
- Symptoms: `IcsDateValue.toInstant` returns `null` for `Date` and for `Floating` when no `floatingTimeZone` is supplied. `CalendarEvent.lastModified` is computed with `toInstant()` and no zone, so any non-UTC `LAST-MODIFIED` value becomes `null`. Sorting or comparing events by start time has no defined behavior for these cases.
- Files: `core/src/commonMain/kotlin/at/zanko/kalenderwecker/ics/models/IcsDateValue.kt`, `core/src/commonMain/kotlin/at/zanko/kalenderwecker/ics/BasicIcsParser.kt` (`createEvent`)
- Trigger: Any all-day event (`VALUE=DATE`) or any event without `Z` or `TZID`.
- Workaround: Callers must pass a `TimeZone` explicitly. There is no documented default.

**Missing DTEND and DURATION are not handled:**
- Symptoms: `end` is nullable and is set only from `DTEND`. A `DURATION` property is ignored. For all-day events, the RFC 5545 exclusive `DTEND` is not adjusted.
- Files: `core/src/commonMain/kotlin/at/zanko/kalenderwecker/ics/BasicIcsParser.kt` (`createEvent`)
- Trigger: Events with only `DTSTART` and `DURATION`, or all-day events with an exclusive end date.
- Workaround: None. Compute the end from `DURATION`, and subtract one day from exclusive all-day end dates when displaying.

## Security Considerations

**Server exposed on all interfaces with no authentication or TLS:**
- Risk: `embeddedServer(Netty, port = 8080, host = "0.0.0.0", ...)` listens on every network interface over plain HTTP. There is no authentication, no TLS, and no rate limiting. The only route is `GET /`.
- Files: `server/src/main/kotlin/at/zanko/kalenderwecker/Application.kt`
- Current mitigation: The route only returns a greeting. No user data is handled yet.
- Recommendations: Bind to `127.0.0.1` for local development, make host and port configurable through environment variables, and put a TLS-terminating reverse proxy in front before any calendar data or credentials pass through the server.

**Untrusted ICS input is parsed without limits:**
- Risk: `parse` reads the whole feed into memory, splits it into lines, and builds intermediate lists and strings. There is no maximum size, maximum event count, or maximum line length. A large or hostile feed can exhaust memory or CPU. Once a network fetch exists, this becomes remotely triggered.
- Files: `core/src/commonMain/kotlin/at/zanko/kalenderwecker/ics/BasicIcsParser.kt` (`parse`, `unfold`)
- Current mitigation: None. The parser is not yet called from any network path.
- Recommendations: Enforce a size limit on the fetched body before parsing, cap the number of events, and reject lines above a fixed length. Use streaming line reads if feeds can be large.

**Credential-like calendar URL in version control:**
- Risk: The TimeEdit subscription URL in `Stuff.md` grants read access to a personal timetable to anyone who can read the repository.
- Files: `Stuff.md`
- Current mitigation: None.
- Recommendations: See the Tech Debt section. Rotate the link and keep it out of git.

**Android backups enabled:**
- Risk: `allowBackup="true"` in the Android manifest allows device backups to include app data. Once calendar data is cached locally, it will be included in backups by default.
- Files: `app/androidApp/src/main/AndroidManifest.xml`
- Current mitigation: No app data is stored yet.
- Recommendations: Set `allowBackup="false"` or provide explicit backup rules before caching any calendar data.

**No content security policy for the web app:**
- Risk: `app/webApp/src/webMain/resources/index.html` has no `Content-Security-Policy` meta tag and loads `webApp.js` without integrity checking. Calendar text will be rendered in the DOM once the UI is implemented, which raises the cost of any injection.
- Files: `app/webApp/src/webMain/resources/index.html`
- Current mitigation: Compose renders into a canvas for the Compose target, so DOM injection is limited today.
- Recommendations: Add a restrictive CSP before any feed content is rendered in HTML or DOM text.

**Network permission not yet declared:**
- Risk: The Android manifest declares no `INTERNET` permission. When feed fetching is added, the permission will be added without a review of what is fetched or from where.
- Files: `app/androidApp/src/main/AndroidManifest.xml`
- Current mitigation: No network code exists in any target (confirmed by searching for Ktor client, `URL(`, and `http` usage).
- Recommendations: Add the permission together with an explicit HTTPS-only URL policy in the same change.

## Performance Bottlenecks

**Regex compiled on every date parse:**
- Problem: `parseIcsDate` builds `Regex(...)` objects inside the function on every call, including the `\d{8}` pattern and the DATE-TIME pattern. A feed with thousands of events compiles the same expressions thousands of times.
- Files: `core/src/commonMain/kotlin/at/zanko/kalenderwecker/ics/BasicIcsParser.kt` (`parseIcsDate`)
- Cause: Patterns are inlined as literals instead of being held in top-level or companion `val`s.
- Improvement path: Hoist the regexes to private top-level `val`s.

**Repeated linear scans and list copies during parsing:**
- Problem: `parse` uses `"BEGIN:VCALENDAR" in lines` and `"END:VCALENDAR" in lines`, which scan the whole list. `unfold` appends to the last element with `result[result.lastIndex] += ...`, which copies the string each time a long folded line is extended.
- Files: `core/src/commonMain/kotlin/at/zanko/kalenderwecker/ics/BasicIcsParser.kt` (`parse`, `unfold`)
- Cause: Immutable string concatenation in a loop, and whole-list membership tests.
- Improvement path: Use a `StringBuilder` for folded lines and a single pass for the header and footer checks.

**Load-more triggered during composition:**
- Problem: `InfiniteScrollList` calls `loadMoreItems()` inside the `items { }` builder when the index is the last one. Composition is not the place for side effects, so this can fire repeatedly across recompositions and does not trigger on scroll position.
- Files: `app/shared/src/commonMain/kotlin/at/zanko/kalenderwecker/InfiniteScrollList.kt`
- Cause: Side effect placed in the item lambda with no `LaunchedEffect` or `snapshotFlow` on `listState`. The list also has no `key` parameter, so item state is not preserved when the list changes.
- Improvement path: Observe `listState.layoutInfo` with `snapshotFlow` in a `LaunchedEffect`, trigger loads when the visible index approaches the end, and pass a stable `key` to `items`.

## Fragile Areas

**The ICS parser is the only real logic and is shared by all targets:**
- Files: `core/src/commonMain/kotlin/at/zanko/kalenderwecker/ics/BasicIcsParser.kt`, `core/src/commonMain/kotlin/at/zanko/kalenderwecker/ics/models/IcsDateValue.kt`
- Why fragile: It depends on `kotlinx-datetime` 0.8.0 for `LocalDate`, `LocalDateTime`, `TimeZone`, and `toInstant`. Time zone lookup depends on each platform's tz database (JVM, JS, Wasm, and iOS differ). Any parsing change affects all five targets at once.
- Safe modification: Add tests for each date form and each escape before changing parse logic. Run `./gradlew :core:jvmTest :core:wasmJsTest :core:jsTest` after each change.
- Test coverage: Only `BasicIcsParserTest` exists, and it asserts only an event count of 3 for `TimeEdit_UltraShort.ics`.

**Property map keyed by name with last-write-wins:**
- Files: `core/src/commonMain/kotlin/at/zanko/kalenderwecker/ics/BasicIcsParser.kt` (`parse`, `Property`, `parseProperty`)
- Why fragile: Repeated properties (for example multiple `ATTENDEE` or `CATEGORIES` lines, or nested `VALARM` properties) overwrite each other silently. Parameter values with quoted `;` or `:` are split incorrectly because `parseProperty` splits the header on `;` and takes the first `:` as the value separator.
- Safe modification: Keep multi-valued properties in lists. Add tests with quoted parameter values before changing the splitter.
- Test coverage: None for quoted parameters, repeated properties, or nested components.

**Template UI depends on the shared Greeting and platform stubs:**
- Files: `app/shared/src/commonMain/kotlin/at/zanko/kalenderwecker/App.kt`, `app/shared/src/commonMain/kotlin/at/zanko/kalenderwecker/Greeting.kt`, `core/src/commonMain/kotlin/at/zanko/kalenderwecker/GreetingUtil.kt`, `server/src/main/kotlin/at/zanko/kalenderwecker/Application.kt`
- Why fragile: The server's `module()` and the shared `App()` both depend on `sayHello`, which lives in `core`. Changing `sayHello` affects the server response that `ApplicationTest` asserts as `"Hello, Ktor!"`.
- Safe modification: Replace these together, and update `ApplicationTest` and `PortConfigTest` in the same change.
- Test coverage: The server test covers only the root route.

**Gradle configuration cache and multi-target build:**
- Files: `gradle.properties`, `settings.gradle.kts`, `build.gradle.kts`
- Why fragile: `org.gradle.configuration-cache=true` is enabled while the build uses several Kotlin Multiplatform plugins and a Compose hot-reload run configuration (`.run/app.desktopApp [hot] 🔥.run.xml`). Plugins that are incompatible with the configuration cache fail only on the second run.
- Safe modification: After changing any plugin or build script, run a clean build twice to exercise the cache.

**Pre-release UI dependency:**
- Files: `gradle/libs.versions.toml`
- Why fragile: `material3 = "1.12.0-alpha03"` is an alpha release, and its API can change between alpha builds.
- Safe modification: Pin it explicitly and check release notes before bumping.

## Scaling Limits

**Single feed, single user, in-memory parsing:**
- Current capacity: The parser holds one complete feed as a `String` and a `List<String>` of unfolded lines. The largest checked-in fixture is 487 lines.
- Limit: Not measured. Memory use scales with feed size and no ceiling is enforced.
- Scaling path: Stream the feed line by line, cap the input size, and parse on a background dispatcher when the feed fetch exists.

## Dependencies at Risk

**`material3` pinned to an alpha:**
- Risk: `gradle/libs.versions.toml` pins `material3 = "1.12.0-alpha03"` while the other Compose libraries use `composeMultiplatform = "1.12.2"`. The version skew means the Material theme and the Compose runtime are not from the same release train.
- Impact: Breaking changes in material3 can break `App()` on every target with no warning.
- Migration plan: Align material3 with the Compose Multiplatform release line, or document why the alpha is required.

**Unused Koin and mockk entries:**
- Risk: Declared in `gradle/libs.versions.toml` but not referenced. Dependency scanners will flag them without any code using them.
- Impact: Noise in security reports and version bumps.
- Migration plan: Remove until needed. See Tech Debt.

**Nix flake tracks nixos-unstable:**
- Risk: `flake.nix` uses `nixos-unstable` as its input. The lock file pins it, but each `nix flake update` moves the JDK, Gradle, and Kotlin versions together.
- Impact: Toolchain drift between developer machines that have not updated the lock file in step with each other.
- Migration plan: Keep `flake.lock` committed and update it deliberately. The uncommitted change to `flake.lock` in the working tree should be reviewed before commit.

## Missing Critical Features

**No calendar fetch path:**
- Problem: Nothing in the app or server fetches an ICS feed. `DataHandler.kt` is empty and the UI is the template.
- Blocks: Any end-to-end use of the parser, offline caching, and the calendar UI.

**No recurrence expansion:**
- Problem: `RRULE`, `EXDATE`, and `RECURRENCE-ID` overrides are not applied. Only base `VEVENT` blocks are returned (see Known Bugs).
- Blocks: A correct weekly or daily schedule view.

**No time zone mapping:**
- Problem: Windows zone names and missing zones are not handled (see Known Bugs).
- Blocks: Displaying events from non-IANA exports correctly.

## Test Coverage Gaps

**ICS parser escapes, folding, and date forms:**
- What's not tested: `unescape` for each sequence, line unfolding with tabs and spaces, `VALUE=DATE` events, `TZID` events, `Z` events, `Floating` events, `LAST-MODIFIED` handling, and `RECURRENCE-ID`.
- Files: `core/src/commonTest/kotlin/at/zanko/kalenderwecker/BasicIcsParserTest.kt`
- Risk: The escaping bug and the time zone failures above are invisible to the current suite.
- Priority: High

**Malformed feed behavior:**
- What's not tested: Missing `UID`, missing `DTSTART`, invalid dates, lowercase or padded headers, and truncated feeds.
- Files: `core/src/commonTest/kotlin/at/zanko/kalenderwecker/BasicIcsParserTest.kt`
- Risk: One bad event aborts the whole feed in production with no test noticing.
- Priority: High

**Nested components:**
- What's not tested: `VALARM` inside `VEVENT`, and the leak of its properties into the event.
- Files: `core/src/commonMain/kotlin/at/zanko/kalenderwecker/ics/BasicIcsParser.kt`
- Risk: Event descriptions are silently replaced by alarm descriptions on feeds that include alarms.
- Priority: High

**Larger fixture unused:**
- What's not tested: `TimeEdit_Software_Engineering_for_AI_...ics` (43 events, DATE and DATE-TIME values) is present in `core/src/commonTest/resources/` but no test reads it.
- Files: `core/src/commonTest/resources/TimeEdit_Software_Engineering_for_AI_master__TASEF-26H-ML026_2026-11-04_10_27.ics`
- Risk: Realistic data shape is untested.
- Priority: Medium

**Shared app, UI, and server behavior:**
- What's not tested: `App()`, `InfiniteScrollList`, and every platform entry point (`app/desktopApp/src/main/kotlin/at/zanko/kalenderwecker/main.kt`, `app/webApp/src/webMain/kotlin/at/zanko/kalenderwecker/main.kt`, `app/androidApp/src/main/kotlin/at/zanko/kalenderwecker/MainActivity.kt`). Only the root server route is exercised.
- Files: `app/shared/src/commonMain/kotlin/at/zanko/kalenderwecker/App.kt`, `app/shared/src/commonMain/kotlin/at/zanko/kalenderwecker/InfiniteScrollList.kt`, `server/src/test/kotlin/at/zanko/kalenderwecker/ApplicationTest.kt`
- Risk: UI regressions and load-more behavior go unnoticed.
- Priority: Medium

---

*Concerns audit: 2026-11-27*
