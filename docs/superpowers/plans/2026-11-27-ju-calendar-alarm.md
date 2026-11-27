# KalenderWecker Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** A TimeEdit-driven wake-up alarm app (Android + Linux desktop) that rings a fixed offset before the first morning class, or at a fallback time otherwise.

**Architecture:** All rules live in `core` as pure, unit-tested code (`AlarmPlanner`, repositories, `AlarmSyncer`). `app/shared` holds Compose UI + ViewModels wired with Koin. Platforms only implement `AlarmScheduler`: Android via `AlarmManager.setAlarmClock`, desktop via generated systemd user timers that launch the app in `--ring` mode.

**Tech Stack:** Kotlin 2.4 KMP, Compose Multiplatform 1.12, Material 3 (Expressive), Koin 4.2, Ktor client 3.6, kotlinx-serialization 1.13, kotlinx-datetime 0.8, multiplatform-settings 1.3, FileKit 0.17, WorkManager 2.12, mp3spi/vorbisspi, coroutines-test + Turbine.

**Spec:** `docs/superpowers/specs/2026-11-27-ju-calendar-alarm-design.md`

## Global Constraints

- Package root `at.zanko.kalenderwecker`; Kotlin official code style.
- Defaults: offset **90 min**, morning ends **12:00**, fallback **08:00**, fallback days **Mon–Fri**, horizon **14 days**, snooze **5 min**, refresh every **6 h**.
- Times come from the device zone (`TimeZone.currentSystemDefault()`), injected as `() -> TimeZone` so tests pin `Europe/Stockholm`.
- Clock injected as `kotlin.time.Clock`; no `Clock.System` calls outside Koin modules / entry points.
- iOS, js, wasmJs targets must keep compiling (no-op/`expect` stubs are fine); server untouched.
- Tests follow the existing name style `gIVEN_x_WHEN_y_THEN_z`; run core/shared tests on JVM (`:core:jvmTest`, `:app:shared:jvmTest`).
- Each commit ends with `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.

## Review Focus

1. User pastes the TimeEdit **HTML** link or a `webcal://` link → normalized to the `https://….ics` URL (Task 5).
2. Offline / TimeEdit down → last cached schedule stays visible and alarms stay scheduled; error shown, not a crash (Tasks 5, 6).
3. DST change days in Europe/Stockholm (2026-03-29, 2026-10-25) → alarm at correct local wall time (Task 3).
4. `STATUS:CANCELLED` and all-day (`VALUE=DATE`) events never produce an alarm (Task 2).
5. Desktop timer fires much later than planned (resume from suspend) or alarm was toggled off after the timer was written → `--ring` recomputes and stays silent (> 2 h late / disabled) (Task 10).

---

### Task 1: Nix flake tooling

**Files:**
- Modify: `flake.nix`, `flake.lock`

- [ ] **Step 1:** In the overlay also override `gradle_9` with the JDK; add to `packages`: `jdk` (`pkgs."jdk${toString javaVersion}"`), `android-tools`, `systemd` is host-provided (don't add). Add `env`/`shellHook`: `JAVA_HOME = jdk.home`, `LD_LIBRARY_PATH = lib.makeLibraryPath [ libGL libx11 libxext libxrender libxtst libxi libxrandr fontconfig freetype alsa-lib libpulseaudio ]` (Compose Desktop / javax.sound on NixOS).
- [ ] **Step 2:** Reload: `nix flake check` then `direnv reload` (or `nix develop -c true`). Expected: no eval errors; `nix develop -c java -version` prints 25.
- [ ] **Step 3:** `nix develop -c ./gradlew :core:jvmTest` → BUILD SUCCESSFUL.
- [ ] **Step 4:** Commit `build(nix): add JDK, android-tools and desktop native libs to dev shell`.

### Task 2: Dependencies, parser class, Appointment mapping

**Files:**
- Modify: `gradle/libs.versions.toml`, `core/build.gradle.kts`, `app/shared/build.gradle.kts`, `app/desktopApp/build.gradle.kts`, `app/androidApp/build.gradle.kts`, root `build.gradle.kts` (serialization plugin)
- Create: `core/src/commonMain/kotlin/at/zanko/kalenderwecker/ics/BasicIcsParser.kt` (class added to existing file), `core/src/commonMain/kotlin/at/zanko/kalenderwecker/model/Appointment.kt`
- Delete: `core/.../ics/DataHandler.kt`, `core/.../GreetingUtil.kt` (+ `app/shared/.../Greeting.kt` usage removed in Task 9)
- Test: `core/src/commonTest/kotlin/at/zanko/kalenderwecker/AppointmentMappingTest.kt`

**Interfaces:**
- Produces: `class BasicIcsParser : CalendarParser`; `data class Appointment(val key: String, val title: String, val location: String?, val description: String?, val start: Instant, val end: Instant?)`; `fun CalendarEvent.toAppointment(zone: TimeZone): Appointment?`; `fun List<CalendarEvent>.toAppointments(zone: TimeZone): List<Appointment>` (sorted by start).

- [ ] **Step 1:** Catalog: material3 → `1.13.0-rc01`; add ktor-client-core/okhttp/js/darwin/mock `3.6.2` (bump `ktor`), kotlinx-serialization-json `1.13.0` + plugin, `com.russhwolf:multiplatform-settings-no-arg` / `-test` `1.3.0`, filekit-dialogs-compose `0.17.1`, coroutines-test `1.11.0`, turbine `1.2.1`, work-runtime-ktx `2.12.0`, koin-test, `com.googlecode.soundlibs:mp3spi:1.9.5.4`, `com.googlecode.soundlibs:vorbisspi:1.0.3.3`, `org.jetbrains.compose.material:material-icons-extended:1.7.3`. Wire core: ktor core + engines per target, serialization, settings, koin-core; core test: ktor-mock, coroutines-test, settings-test, turbine.
- [ ] **Step 2: Failing tests** in `AppointmentMappingTest`:
  - `gIVEN_utcEvent_WHEN_toAppointment_THEN_startIsInstant`: from `TimeEdit_UltraShort.ics`, first appointment start == `Instant.parse("2026-11-02T07:15:00Z")`, title/location carried over.
  - `gIVEN_cancelledEvent_WHEN_toAppointment_THEN_null` (`STATUS:CANCELLED`).
  - `gIVEN_allDayEvent_WHEN_toAppointment_THEN_null` (`DTSTART;VALUE=DATE:20261102`).
  - `gIVEN_floatingEvent_WHEN_toAppointment_THEN_interpretedInZone` (Europe/Stockholm, `20261102T081500` → `07:15Z`).
  - `gIVEN_parserClass_WHEN_parse_THEN_sameAsFunction` (BasicIcsParser on UltraShort → 3 events).
- [ ] **Step 3:** Run `./gradlew :core:jvmTest` → FAIL (unresolved).
- [ ] **Step 4:** Implement. `key = eventKey`; `Zoned` with unknown TZID falls back to `zone`.
- [ ] **Step 5:** `./gradlew :core:jvmTest :core:compileKotlinJs :core:compileKotlinWasmJs` → PASS. Commit `feat(core): Appointment model and BasicIcsParser class`.

### Task 3: AlarmSettings + AlarmPlanner

**Files:**
- Create: `core/src/commonMain/kotlin/at/zanko/kalenderwecker/alarm/AlarmSettings.kt`, `.../alarm/DayAlarm.kt`, `.../alarm/AlarmPlanner.kt`
- Test: `core/src/commonTest/kotlin/at/zanko/kalenderwecker/AlarmPlannerTest.kt`

**Interfaces:**
- Produces:
  - `@Serializable data class AlarmSettings(val icsUrl: String = "", val offsetMinutes: Int = 90, val morningEnd: LocalTime = LocalTime(12, 0), val fallbackTime: LocalTime = LocalTime(8, 0), val fallbackDays: Set<DayOfWeek> = MONDAY..FRIDAY, val soundUri: String? = null, val horizonDays: Int = 14)`
  - `sealed interface AlarmSource { data class BeforeAppointment(val appointment: Appointment) : AlarmSource; data object Fallback : AlarmSource }`
  - `data class DayAlarm(val date: LocalDate, val time: Instant, val source: AlarmSource, val enabled: Boolean)`
  - `class AlarmPlanner { fun plan(appointments: List<Appointment>, settings: AlarmSettings, overrides: Map<LocalDate, Boolean>, now: Instant, zone: TimeZone): List<DayAlarm> }`

Rules: one entry per date from `today(zone)` to `today + horizonDays - 1`. First appointment of that local date starting before `morningEnd` → `BeforeAppointment`, time = start − offset, default enabled `true`. Otherwise `Fallback` at `date.atTime(fallbackTime).toInstant(zone)`, default enabled = `dayOfWeek in fallbackDays` (so a weekend fallback exists and can be switched on). `enabled = overrides[date] ?: default`. Entries with `time <= now` are dropped. Sorted by date.

- [ ] **Step 1: Failing tests** (zone Europe/Stockholm, `now = 2026-11-01T20:00Z` Sunday unless stated):
  - `gIVEN_morningClass_WHEN_plan_THEN_alarmOffsetBeforeStart`: class 2026-11-02 08:15 local → alarm `06:45` local, source BeforeAppointment.
  - `gIVEN_twoMorningClasses_WHEN_plan_THEN_usesEarliest`.
  - `gIVEN_onlyAfternoonClass_WHEN_plan_THEN_fallback`: class 13:15 → Fallback 08:00, enabled.
  - `gIVEN_classExactlyAtMorningEnd_WHEN_plan_THEN_fallback` (12:00 is not morning).
  - `gIVEN_saturdayNoClass_WHEN_plan_THEN_fallbackDisabledByDefault`.
  - `gIVEN_override_WHEN_plan_THEN_overrideWins` (both directions).
  - `gIVEN_horizon14_WHEN_plan_THEN_14Days` (now before today's alarm) and `gIVEN_todaysAlarmPassed_WHEN_plan_THEN_todayDropped`.
  - `gIVEN_offsetCrossesMidnight_WHEN_plan_THEN_dateIsClassDate` (class 00:30, alarm previous evening, `date` = class date).
  - `gIVEN_dstSpringForward_WHEN_plan_THEN_localWallTimeKept` (2026-03-30 class 08:15 → alarm 06:45 local = 04:45Z; also 2026-10-26 → 05:45Z).
- [ ] **Step 2:** Run → FAIL. **Step 3:** Implement. **Step 4:** Run → PASS.
- [ ] **Step 5:** Commit `feat(core): AlarmPlanner computes one wake-up alarm per day`.

### Task 4: SettingsRepository

**Files:**
- Create: `core/src/commonMain/kotlin/at/zanko/kalenderwecker/data/SettingsRepository.kt`
- Test: `core/src/commonTest/kotlin/at/zanko/kalenderwecker/SettingsRepositoryTest.kt`

**Interfaces:**
- Consumes: `AlarmSettings` (Task 3), `com.russhwolf.settings.Settings`.
- Produces: `class SettingsRepository(private val store: Settings)` with `val settings: StateFlow<AlarmSettings>`, `fun update(transform: (AlarmSettings) -> AlarmSettings)`, `val overrides: StateFlow<Map<LocalDate, Boolean>>`, `fun setEnabled(date: LocalDate, enabled: Boolean)`, `var cachedIcs: String?`, `fun reload()` (re-read store; used after another process wrote it). Stored as JSON strings under keys `settings`, `overrides`, `cachedIcs`.

- [ ] **Step 1: Failing tests** with `MapSettings()`: defaults when empty; `update` persists (new repo on same store sees it); `setEnabled` persists and emits; corrupt JSON in `settings` → defaults, no throw.
- [ ] **Step 2–4:** FAIL → implement → PASS. **Step 5:** Commit `feat(core): persistent SettingsRepository`.

### Task 5: CalendarRepository + URL normalization

**Files:**
- Create: `core/src/commonMain/kotlin/at/zanko/kalenderwecker/data/CalendarRepository.kt`, `.../data/TimeEditUrl.kt`
- Test: `core/src/commonTest/kotlin/at/zanko/kalenderwecker/CalendarRepositoryTest.kt`, `TimeEditUrlTest.kt`

**Interfaces:**
- Consumes: `CalendarParser`, `SettingsRepository`, `toAppointments`.
- Produces: `fun normalizeTimeEditUrl(input: String): String?` (trim; `webcal://`→`https://`; `.html` suffix → `.ics`; null unless `http(s)` and ends with `.ics`); `class CalendarRepository(client: HttpClient, parser: CalendarParser, settings: SettingsRepository, zone: () -> TimeZone)` with `val appointments: StateFlow<List<Appointment>>` (seeded from `cachedIcs`) and `suspend fun refresh(): Result<Unit>`; failure types `sealed class RefreshError : Exception { NoUrl, InvalidUrl, Network(cause), Http(status), Parse(cause) }`.

- [ ] **Step 1: Failing tests:** URL cases (html, webcal, whitespace, garbage → null, the `Stuff.md` URL unchanged). Repository with `MockEngine`: success updates appointments + `cachedIcs`; HTTP 500 → `Result.failure(Http)` and previous appointments kept; network exception → `Network`, cache kept; invalid body → `Parse`, cache kept; empty url → `NoUrl` without a request; new repo over a store with cached ICS → appointments non-empty before any refresh.
- [ ] **Step 2–4:** FAIL → implement → PASS. **Step 5:** Commit `feat(core): fetch and cache TimeEdit calendar`.

### Task 6: AlarmScheduler contract, AlarmSyncer, Koin core module

**Files:**
- Create: `core/.../alarm/AlarmScheduler.kt`, `core/.../alarm/AlarmSyncer.kt`, `core/.../di/CoreModule.kt`
- Test: `core/src/commonTest/kotlin/at/zanko/kalenderwecker/AlarmSyncerTest.kt`, `CoreModuleTest.kt`

**Interfaces:**
- Produces:
  - `interface AlarmScheduler { suspend fun schedule(alarms: List<DayAlarm>) }` — contract: replace everything previously scheduled with exactly `alarms` (all enabled).
  - `class AlarmSyncer(calendar: CalendarRepository, settings: SettingsRepository, planner: AlarmPlanner, scheduler: AlarmScheduler, clock: Clock, zone: () -> TimeZone)` with `fun currentPlan(): List<DayAlarm>`, `val plan: Flow<List<DayAlarm>>` (combine of appointments/settings/overrides), `suspend fun sync(): List<DayAlarm>` (schedules enabled subset, returns full plan), `suspend fun refreshAndSync(): Result<Unit>` (sync runs even when refresh fails), `fun alarmFor(date: LocalDate): DayAlarm?`.
  - `val coreModule: Module` (HttpClient, BasicIcsParser as CalendarParser, `Settings()`, repos, planner, syncer, `Clock.System`, `{ TimeZone.currentSystemDefault() }`); `AlarmScheduler` must come from a platform module.

- [ ] **Step 1: Failing tests:** fake scheduler records calls; sync passes only enabled alarms; toggling off a date and syncing removes it; refresh failure still calls scheduler with cached plan and returns failure; `CoreModuleTest`: `koinApplication { modules(coreModule, module { single<AlarmScheduler> { fake } ; single<Settings> { MapSettings() } }) }.koin.get<AlarmSyncer>()` resolves.
- [ ] **Step 2–4:** FAIL → implement → PASS. **Step 5:** Commit `feat(core): AlarmSyncer and Koin core module`.

### Task 7: HomeViewModel

**Files:**
- Create: `app/shared/src/commonMain/kotlin/at/zanko/kalenderwecker/ui/home/HomeViewModel.kt`, `.../ui/home/DaySection.kt`, `app/shared/.../di/AppModule.kt` (`val appModule`, `fun initKoin(vararg platform: Module): KoinApplication`)
- Test: `app/shared/src/commonTest/kotlin/at/zanko/kalenderwecker/HomeViewModelTest.kt`, `DaySectionTest.kt`

**Interfaces:**
- Consumes: `AlarmSyncer`, `CalendarRepository`, `SettingsRepository`.
- Produces: `data class DaySection(val date: LocalDate, val alarm: DayAlarm?, val appointments: List<Appointment>)`; `fun buildDaySections(alarms: List<DayAlarm>, appointments: List<Appointment>, zone: TimeZone, today: LocalDate): List<DaySection>` (every day with an alarm or an appointment ≥ today, ascending); `data class HomeUiState(val sections: List<DaySection> = emptyList(), val nextAlarm: DayAlarm? = null, val isRefreshing: Boolean = false, val error: RefreshError? = null, val hasUrl: Boolean = false)`; `class HomeViewModel(syncer, calendar, settings, clock, zone) : ViewModel` with `val state: StateFlow<HomeUiState>`, `fun refresh()`, `fun setAlarmEnabled(date: LocalDate, enabled: Boolean)` (persists + `sync()`), `fun dismissError()`. Init triggers `refresh()` when `hasUrl`.

- [ ] **Step 1: Failing tests:** `buildDaySections` groups appointments by local date and attaches the alarm; days beyond the last alarm but with appointments still listed; `nextAlarm` = first enabled; toggle calls scheduler without that date; refresh failure → `error = Network`, `isRefreshing` back to false (Turbine + `StandardTestDispatcher`, `Dispatchers.setMain`).
- [ ] **Step 2–4:** FAIL → implement → PASS (`./gradlew :app:shared:jvmTest`). **Step 5:** Commit `feat(app): HomeViewModel with day sections`.

### Task 8: SettingsViewModel

**Files:**
- Create: `app/shared/.../ui/settings/SettingsViewModel.kt`
- Test: `app/shared/src/commonTest/kotlin/at/zanko/kalenderwecker/SettingsViewModelTest.kt`

**Interfaces:**
- Produces: `class SettingsViewModel(settings: SettingsRepository, syncer: AlarmSyncer) : ViewModel` with `val state: StateFlow<AlarmSettings>`, `val urlError: StateFlow<Boolean>`, `fun setUrl(raw: String)` (stores normalized URL; invalid → `urlError = true`, not stored; valid → `refreshAndSync()`), `fun setOffsetMinutes(m: Int)` (coerce `0..720`), `fun setMorningEnd(t: LocalTime)`, `fun setFallbackTime(t: LocalTime)`, `fun toggleFallbackDay(d: DayOfWeek)`, `fun setSound(uri: String?)`. Every change except URL calls `sync()`.

- [ ] **Step 1: Failing tests:** html URL stored as `.ics`; garbage URL sets `urlError`, store unchanged; offset 1000 → 720; toggling SATURDAY adds it; each change triggers fake scheduler.
- [ ] **Step 2–4:** FAIL → implement → PASS. **Step 5:** Commit `feat(app): SettingsViewModel`.

### Task 9: Material 3 Expressive UI

**Files:**
- Create: `app/shared/.../ui/theme/Theme.kt` (+ `expect @Composable fun dynamicColorScheme(dark: Boolean): ColorScheme?` with android actual = `dynamicDark/LightColorScheme`, others `null`), `ui/home/HomeScreen.kt`, `ui/settings/SettingsScreen.kt`, `ui/ring/RingScreen.kt`, `ui/Format.kt`
- Modify: `app/shared/.../App.kt` (replace template), delete `Greeting.kt`, `InfiniteScrollList.kt`, `composeResources/drawable/compose-multiplatform.xml`; placeholder platform tests that reference `Greeting`.
- Test: `app/shared/src/commonTest/kotlin/at/zanko/kalenderwecker/FormatTest.kt`

**Interfaces:**
- Consumes: Tasks 7–8 ViewModels via `koinViewModel()`.
- Produces: `@Composable fun App()`; `@Composable fun KalenderWeckerTheme(content: @Composable () -> Unit)` (`MaterialExpressiveTheme`, `@OptIn(ExperimentalMaterial3ExpressiveApi::class)`); `@Composable fun RingScreen(time: String, title: String, subtitle: String?, onDismiss: () -> Unit, onSnooze: () -> Unit)`; `fun formatOffset(minutes: Int): String` ("1 h 30 min"), `fun formatAlarmSubtitle(alarm: DayAlarm, zone): String` ("90 min before Data Science" / "Fallback alarm").

- [ ] **Step 1:** Failing `FormatTest` for `formatOffset(90) == "1 h 30 min"`, `formatOffset(45) == "45 min"`, subtitles. → implement → PASS.
- [ ] **Step 2:** Home: `LargeFlexibleTopAppBar` (title "KalenderWecker", settings action), hero card for `nextAlarm`, `PullToRefreshBox` + `LazyColumn` with `stickyHeader` per `DaySection` (date + alarm row with `Switch`), `ElevatedCard` per appointment (time range, title, location), error `Snackbar`, empty state with "Add TimeEdit link" button. Settings: URL `OutlinedTextField` (error text), offset slider (0–180 step 5) + exact value, `TimePicker` dialogs for morning end / fallback, `FilterChip` row for fallback days, sound row with FileKit `rememberFilePickerLauncher(type = FileKitType.File(listOf("mp3","wav","ogg","m4a")))` mapped through `expect fun PlatformFile.persistableSoundUri(): String`. Navigation: `var screen by rememberSaveable` two-state switch with `AnimatedContent`, back handler on Android.
- [ ] **Step 3:** `./gradlew :app:shared:jvmTest :app:shared:compileKotlinJs :app:shared:compileKotlinWasmJs` → PASS. Commit `feat(app): Material 3 Expressive home, settings and ring screens`.

### Task 10: Desktop systemd scheduler and CLI modes

**Files:**
- Create in `app/desktopApp/src/main/kotlin/at/zanko/kalenderwecker/desktop/`: `DesktopMode.kt`, `SystemdUnits.kt`, `SystemdTimerScheduler.kt`, `CommandRunner.kt`, `ExecResolver.kt`, `RingDecision.kt`, `InProcessScheduler.kt`
- Modify: `app/desktopApp/build.gradle.kts` (kotlin-test, coroutines-test for `test`)
- Test: `app/desktopApp/src/test/kotlin/at/zanko/kalenderwecker/desktop/*Test.kt`

**Interfaces:**
- Produces:
  - `sealed interface DesktopMode { data object Main; data class Ring(val date: LocalDate, val force: Boolean); data object Refresh }`, `fun parseArgs(args: Array<String>): DesktopMode` (`--ring 2026-11-30 [--force]`, `--refresh`, else Main).
  - `object SystemdUnits { fun alarmName(date: LocalDate): String /* "kalenderwecker-alarm-2026-11-30" */; fun alarmTimer(alarm: DayAlarm, zone: TimeZone): String; fun alarmService(date: LocalDate, exec: List<String>): String; fun refreshTimer(): String; fun refreshService(exec: List<String>): String }` — timer has `OnCalendar=2026-11-30 06:45:00 Europe/Stockholm`, `Persistent=true`, `AccuracySec=1s`, `[Install] WantedBy=timers.target`; service `Type=oneshot`, `ExecStart=` with each exec arg shell-quoted, plus `--ring <date>`; refresh timer `OnBootSec=2min`, `OnUnitActiveSec=6h`.
  - `fun interface CommandRunner { fun run(vararg cmd: String): Int }`, `class ProcessCommandRunner`.
  - `class SystemdTimerScheduler(unitDir: Path, runner: CommandRunner, exec: List<String>, zone: () -> TimeZone) : AlarmScheduler` — writes files for given alarms, deletes `kalenderwecker-alarm-*.{timer,service}` not in the set (`systemctl --user disable --now` first), always (re)writes refresh units, then `daemon-reload` and `enable --now` each timer. Default `unitDir = ~/.config/systemd/user`.
  - `fun resolveExec(env: Map<String, String>, javaCommand: String?, javaArgs: List<String>): List<String>` — `KALENDERWECKER_EXEC` (split on spaces) wins; else current command + args with any `--ring/--refresh/--force/<date>` args removed.
  - `fun systemdAvailable(runner: CommandRunner): Boolean` (`systemctl --user show-environment` exit 0).
  - `fun shouldRing(alarm: DayAlarm?, now: Instant, force: Boolean): Boolean` — false if null, disabled, or `now - alarm.time > 2.hours`; `force` (snooze) bypasses.
  - `class InProcessScheduler(scope: CoroutineScope, clock: Clock, onRing: (LocalDate) -> Unit) : AlarmScheduler` (fallback when systemd missing).

- [ ] **Step 1: Failing tests:** `parseArgs` cases; `alarmTimer` golden string; exec arg with space quoted; scheduler in a temp dir with fake runner: writes 2 alarms + refresh units, second call with 1 alarm deletes the other's files and issues `disable --now`, last runner calls `daemon-reload` then `enable --now <timer>`; `resolveExec` env precedence + arg stripping; `shouldRing` (on time → true, 3 h late → false, disabled → false, force late → true); `InProcessScheduler` fires `onRing` at the alarm time using `TestScope` virtual time.
- [ ] **Step 2–4:** FAIL → implement → PASS (`./gradlew :app:desktopApp:test`). **Step 5:** Commit `feat(desktop): systemd user timer alarm scheduler`.

### Task 11: Desktop audio and entry point

**Files:**
- Create: `app/desktopApp/.../desktop/DesktopAlarmPlayer.kt`, `.../desktop/DesktopModule.kt`, `.../desktop/RingWindow.kt`
- Modify: `app/desktopApp/.../main.kt`, `app/desktopApp/build.gradle.kts` (mp3spi, vorbisspi, `nativeDistributions` + `targetFormats(Deb, AppImage)`)
- Test: `app/desktopApp/src/test/kotlin/.../DesktopAlarmPlayerTest.kt`

**Interfaces:**
- Produces: `class DesktopAlarmPlayer { fun start(soundPath: String?); fun stop() }` (loops file via `AudioSystem` decoded to PCM; missing/unreadable file or null → `generateBeepPcm()`), `fun generateBeepPcm(sampleRate: Int = 44100): ByteArray` (1 s: 880 Hz beeps 0.2 s on/0.2 s off, 16-bit mono); `val desktopModule` (`AlarmScheduler` = systemd or in-process based on `systemdAvailable`).
- `main(args)`: `initKoin(desktopModule)`; `Main` → window with `App()`, `syncer.sync()` on start; `Refresh` → `runBlocking { refreshAndSync() }`, exit code 0/1; `Ring` → `settings.reload()`, `shouldRing(syncer.alarmFor(date), now, force)` else exit; ring window (always-on-top, `RingScreen`), player loops, auto-stop after 10 min; Snooze → `systemd-run --user --on-active=5m --unit=kalenderwecker-snooze-<epochSec> <exec> --ring <date> --force`, exit.

- [ ] **Step 1:** Failing tests: `generateBeepPcm().size == 88200`; non-zero samples in first 0.2 s, zeros in 0.2–0.4 s. → implement → PASS.
- [ ] **Step 2:** Manual: `./gradlew :app:desktopApp:run` → set URL from `Stuff.md` → list shows appointments; `systemctl --user list-timers 'kalenderwecker*'` shows alarm + refresh timers. `./gradlew :app:desktopApp:run --args="--ring <next alarm date> --force"` → ring window + sound; Snooze creates `kalenderwecker-snooze-*` transient timer.
- [ ] **Step 3:** Commit `feat(desktop): ring window, audio playback and CLI entry point`.

### Task 12: Android alarm integration

**Files:**
- Create in `app/androidApp/src/main/kotlin/at/zanko/kalenderwecker/android/`: `KalenderWeckerApplication.kt`, `AndroidAlarmScheduler.kt`, `AlarmReceiver.kt`, `AlarmService.kt`, `AlarmActivity.kt`, `BootReceiver.kt`, `RefreshWorker.kt`, `AndroidModule.kt`; `app/shared/src/androidMain/.../SoundUri.android.kt` (takes persistable read permission)
- Modify: `AndroidManifest.xml`, `MainActivity.kt` (FileKit init, request `POST_NOTIFICATIONS`), `app/androidApp/build.gradle.kts` (work-runtime, koin-android), `res/values/themes.xml` (Material3 DayNight NoActionBar)
- Test: `app/androidApp/src/test/kotlin/.../AlarmRequestCodeTest.kt`

**Interfaces:**
- Produces: `fun requestCode(date: LocalDate): Int = date.toEpochDays().toInt()`; `class AndroidAlarmScheduler(context: Context, prefs: SharedPreferences) : AlarmScheduler` — cancels codes stored under `scheduled_codes`, then `setAlarmClock(AlarmClockInfo(time, showIntent→MainActivity), broadcast PendingIntent(AlarmReceiver, extra "date"))`, stores new codes; if `!canScheduleExactAlarms()` falls back to `setAndAllowWhileIdle` and logs.
- `AlarmReceiver` → `startForegroundService(AlarmService)`; service: channel `alarm` (IMPORTANCE_HIGH, no channel sound), notification category ALARM with `setFullScreenIntent(AlarmActivity)`, Dismiss/Snooze actions; `MediaPlayer` `USAGE_ALARM`, looping, sound = settings `soundUri` else `RingtoneManager.TYPE_ALARM` default; vibrate; auto-stop after 10 min; snooze = `setAlarmClock(now+5min)` with extra `force=true`. `AlarmActivity`: `setShowWhenLocked(true)`, `setTurnScreenOn(true)`, shows `RingScreen`.
- Manifest: `INTERNET`, `USE_EXACT_ALARM`, `SCHEDULE_EXACT_ALARM` (maxSdkVersion 32), `POST_NOTIFICATIONS`, `USE_FULL_SCREEN_INTENT`, `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MEDIA_PLAYBACK`, `RECEIVE_BOOT_COMPLETED`, `WAKE_LOCK`, `VIBRATE`; `BootReceiver` for `BOOT_COMPLETED`, `TIME_SET`, `TIMEZONE_CHANGED`, `MY_PACKAGE_REPLACED` → `goAsync` + `syncer.sync()`. `RefreshWorker` periodic 6 h `KEEP`, network constraint.

- [ ] **Step 1:** Failing `AlarmRequestCodeTest` (`2026-11-30` → expected epoch day, distinct per date). → implement → PASS (`./gradlew :app:androidApp:testDebugUnitTest`).
- [ ] **Step 2:** Implement components; `./gradlew :app:androidApp:assembleDebug` → BUILD SUCCESSFUL.
- [ ] **Step 3 (if a device/emulator is attached):** `adb install -r …/app-debug.apk`; set URL; `adb shell dumpsys alarm | grep kalenderwecker` shows the alarm clock.
- [ ] **Step 4:** Commit `feat(android): AlarmManager alarms, ringing service and background refresh`.

### Task 13: Full verification

- [ ] `./gradlew :core:allTests :app:shared:jvmTest :app:desktopApp:test :app:androidApp:testDebugUnitTest :app:androidApp:assembleDebug :app:shared:compileKotlinJs :app:shared:compileKotlinWasmJs` → BUILD SUCCESSFUL.
- [ ] Desktop manual run (Task 11 Step 2) on the real TimeEdit URL.
- [ ] Update `README.md` (run instructions, systemd note, Android permissions). Commit `docs: usage and platform notes`.
