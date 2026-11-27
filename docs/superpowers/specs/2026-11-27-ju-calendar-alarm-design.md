# KalenderWecker — TimeEdit wake-up alarm (design)

Date: 2026-11-27

## Intent

A student at Jönköping University (JU) wants to be woken up in time for class
without hand-setting alarms. They paste their TimeEdit `.ics` URL once; the app
shows the upcoming appointments and keeps one wake-up alarm per day in sync
with the schedule.

## Goals

- Enter a TimeEdit `.ics` URL; fetch and parse the appointments.
- Show upcoming appointments in a scrollable list, grouped by day.
- One alarm per day:
  - if the day's first appointment starts **before "morning ends at"**
    (default 12:00): alarm = first start − **offset** (default 90 min);
  - otherwise: the **fallback alarm** (user-set time, default 08:00).
- Every daily alarm can be switched on/off individually; the choice survives
  schedule refreshes (keyed by date).
- User picks a wake-up audio file; otherwise a default sound is used.
- Material 3 Expressive UI (dynamic colour on Android).
- Runs on **Android** (alarm fires with the app closed) and **desktop**
  (alarm fires while the app runs, kept alive in the system tray).
- Koin for DI, test-driven development, tools from the nix flake.

## Non-goals

- iOS, web and the Ktor server: left compiling as they are, no new features.
- Multiple calendars, editing appointments, accounts/sync.

## Anti-goals

- An alarm that silently doesn't ring (stale schedule, killed process, lost
  permission). Failures must be visible in the UI.

## Constraints

- Existing stack: KMP, Compose Multiplatform, kotlinx-datetime, okio,
  `core/` ICS parser with TimeEdit fixtures (times in UTC).
- Desktop has no OS alarm service → the app process must be running.
- Android 12+ (minSdk 31): exact alarms need `USE_EXACT_ALARM`/
  `SCHEDULE_EXACT_ALARM`, notifications need `POST_NOTIFICATIONS`, ringing over
  the lock screen needs a full-screen-intent notification.

## Approaches considered

1. **Pure planner in `core` + thin platform schedulers (chosen).** All rules
   (which time, fallback, toggles) are a pure function
   `AlarmPlanner.plan(events, settings, overrides, today, zone)` — fully
   unit-testable in `commonTest`. Platforms only implement
   `AlarmScheduler` / `AlarmPlayer`.
2. Platform-native logic (Android WorkManager computes alarms, desktop has its
   own). Duplicates rules per platform, hard to test → rejected.
3. Room/SQLDelight database for events and alarms. More moving parts than the
   data needs (a few hundred events, a handful of settings) → rejected;
   settings and toggles go in `multiplatform-settings` (works on every
   target), the last fetched `.ics` text is cached there too.

## Design

### Modules

- `core` (commonMain, no UI):
  - `BasicIcsParser : CalendarParser` (wraps existing `parse`).
  - `Appointment` (key, title, location, description, start/end `Instant`).
  - `AlarmSettings(icsUrl, offsetMinutes, morningEnd, fallbackTime,
    fallbackDays, soundUri)`.
  - `AlarmPlanner` → `List<DayAlarm(date, time: Instant, source: Appointment|
    Fallback, enabled)>` for the next N days (default 14).
  - `CalendarRepository` (Ktor client fetch → parse → cache; `refresh()`,
    `appointments: Flow`), `SettingsRepository` (settings + per-date
    toggles as `Flow`s).
  - `AlarmSyncer`: settings/appointments/toggles → plan → `AlarmScheduler`.
- `app/shared`: Compose UI, ViewModels (`HomeViewModel`, `SettingsViewModel`),
  Koin modules (`commonModule` + per-platform `platformModule`), interfaces
  `AlarmScheduler`, `AlarmPlayer`, `SoundPicker` with `expect`/actual or
  Koin-provided platform implementations (no-op on iOS/web).
- `app/androidApp`: `AlarmManager.setAlarmClock` scheduler, `AlarmReceiver` →
  foreground `AlarmService` (MediaPlayer, alarm audio attributes, loops) +
  full-screen `AlarmActivity` (Dismiss / Snooze 5 min), `BootReceiver`
  re-schedules, WorkManager periodic refresh (every 6 h). Picked sound via
  SAF with persisted URI permission; default = system alarm ringtone.
- `app/desktopApp` — **systemd user timers (chosen over a resident daemon,
  2026-11-27)**:
  - `SystemdTimerScheduler : AlarmScheduler` writes one
    `kalenderwecker-alarm-<date>.timer/.service` pair per enabled alarm into
    `~/.config/systemd/user/` (`OnCalendar=<local time>`, `Persistent=true`,
    `AccuracySec=1s`), removes stale ones, then `systemctl --user
    daemon-reload` and enables the timers. Unit-file rendering is pure and
    unit-tested; the `systemctl` call sits behind a `CommandRunner` interface.
  - `kalenderwecker-refresh.timer` (`OnBootSec=2min`, `OnUnitActiveSec=6h`)
    runs `--refresh`: fetch, re-plan, rewrite alarm timers (headless).
  - CLI modes of the one executable: no args = main window; `--ring <date>`
    = ringing window only (Dismiss / Snooze 5 min — snooze writes a one-shot
    transient timer via `systemd-run --user --on-active=5m`); `--refresh`.
  - The launch command written into units is taken from a
    `KALENDERWECKER_EXEC` env var / the running process's command line, so
    it works from `./gradlew run`, an installed package, or a nix build.
  - Fallback when `systemctl --user` is unavailable (macOS/Windows/no
    systemd): in-process ticker while the app runs.
  - Playback via `javax.sound` + mp3spi/vorbisspi (WAV/MP3/OGG); default =
    generated tone. Limitation: user timers don't wake the machine from
    suspend.

### UI (Material 3 Expressive)

- **Home:** large top bar; hero card "Next alarm: Tue 06:45 · 90 min before
  Data Science"; `LazyColumn` grouped by day — sticky day header with the
  alarm row (time, "before class"/"fallback", `Switch`), then appointment
  cards (time range, title, room). Pull-to-refresh, error banner on fetch
  failure / missing permission, empty state prompting for the URL.
- **Settings:** TimeEdit URL, offset, morning-ends-at, fallback time,
  fallback days, sound picker (+ preview), permission status on Android.

### Testing (TDD)

- `core` commonTest (run on JVM): planner rules (morning/afternoon/no class,
  offset across midnight, toggles, fallback days, time zone/DST),
  parser→Appointment mapping, repository with Ktor `MockEngine`, settings
  repository with in-memory `MapSettings`, syncer with fake scheduler.
- `app/shared` jvmTest: ViewModels with fakes + coroutines-test/Turbine.
- Platform code (AlarmManager, tray) verified by building and a manual run.

### Tooling

- `flake.nix`: explicit JDK, Gradle on that JDK, `android-tools`, and the
  native libraries Compose Desktop/audio need on NixOS (libGL, X11 libs,
  fontconfig, alsa-lib) via `LD_LIBRARY_PATH`. Reloaded with direnv.
- New catalog entries: ktor-client (core, okhttp/cio, mock), kotlinx
  serialization, multiplatform-settings, filekit, coroutines-test, turbine,
  work-runtime, material-icons; material3 bumped to latest.

## Calls made by the builder (not discussed)

- One alarm per **day**, toggle keyed by date; fallback applies on
  Mon–Fri by default (configurable days).
- Planning horizon 14 days; refresh every 6 h plus on app start / pull.
- Snooze is fixed at 5 min.
- Timezone: device zone (Europe/Stockholm on a JU phone).
