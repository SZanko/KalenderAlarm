---
last_mapped_commit: 178eeade16d92a0598e3d1178742d6bf8e72edfd
last_mapped_at: 2026-11-27
---
# External Integrations

**Analysis Date:** 2026-11-27

## APIs & External Services

**Calendar feed (ICS):**
- TimeEdit calendar export (iCalendar `.ics` format). A TimeEdit feed URL is recorded in `Stuff.md` at the repository root. The URL is not reproduced in this document because it is a personal feed link.
  - SDK/Client: none. No HTTP client code exists in production sources. Parsing is local, in `core/src/commonMain/kotlin/at/zanko/kalenderwecker/ics/BasicIcsParser.kt` (entry: `parse(ics: String)` in the same file, interface `CalendarParser` in `ics/CalendarParser.kt`).
  - Auth: none. The feed link itself acts as the credential, so treat `Stuff.md` as sensitive.
  - Sample data: `core/src/commonTest/resources/TimeEdit_Software_Engineering_for_AI_master__TASEF-26H-ML026_2026-11-04_10_27.ics` (UIDs suffixed `@timeedit.com`), used by `core/src/commonTest/kotlin/at/zanko/kalenderwecker/BasicIcsParserTest.kt`.
  - Current status: ICS text is parsed from strings. Fetching the feed over the network is not implemented.

**Other external APIs:**
- Not detected.

## Data Storage

**Databases:**
- Not detected. No database driver, ORM, or SQL dependency is declared in `gradle/libs.versions.toml` or any `build.gradle.kts`.

**File Storage:**
- Local filesystem only. Test fixtures live in `core/src/commonTest/resources/`.
- Compose resources (images, drawables) are bundled from `app/shared/src/commonMain/composeResources/`.

**Caching:**
- None detected at the application level.
- Gradle build cache enabled (`gradle.properties`: `org.gradle.caching=true`). This is local build caching, not runtime caching.

## Authentication & Identity

**Auth Provider:**
- Not applicable. No user authentication, sessions, or identity library is present.
- The server (`server/src/main/kotlin/at/zanko/kalenderwecker/Application.kt`) exposes only `GET /` and has no auth plugin installed.

## Monitoring & Observability

**Error Tracking:**
- None detected (no Sentry, Crashlytics, or similar SDK in the version catalog).

**Logs:**
- Server: Logback 1.6.6 via `server/src/main/resources/logback.xml`, ConsoleAppender to STDOUT, root level `trace`, `org.eclipse.jetty` and `io.netty` at INFO.
- Client apps: `println`/platform default output. No logging framework is declared for `core` or `app/shared`.

## CI/CD & Deployment

**Hosting:**
- Not detected. No deployment configuration for the server (no Dockerfile, Kubernetes, Procfile, or cloud config).
- Android: distribution artifact is an APK/AAB built from `app/androidApp/` (`applicationId` `at.zanko.kalenderwecker`, no signing config in the module).
- Desktop: native installers via Compose Desktop `nativeDistributions` in `app/desktopApp/build.gradle.kts`.
- Web: static bundle from `app/webApp/` (`webApp.js` plus `index.html`).

**CI Pipeline:**
- Not detected. No `.github/workflows/`, `.gitlab-ci.yml`, or similar files exist in the repository.

**Dependency sources:**
- Google Maven (`google { mavenContent { ... } }`) for androidx, com.android, and com.google groups
- Maven Central
- Gradle Plugin Portal
- foojay Disco API for JDK toolchain provisioning (`gradle/gradle-daemon-jvm.properties`, `api.foojay.io`)
- `services.gradle.org` for the Gradle distribution (`gradle/wrapper/gradle-wrapper.properties`)
- `channels.nixos.org` for nixpkgs (`flake.nix`)

## Environment Configuration

**Required env vars:**
- None read by application code.
- `ANDROID_HOME` or `local.properties` `sdk.dir` is needed for Android builds (build tooling, not application code).

**Secrets location:**
- No secrets are required by the current code. The TimeEdit feed URL in `Stuff.md` is effectively a secret credential and is committed to git (`Stuff.md` is not in `.gitignore`). Consider moving it to an untracked file or environment variable before a real fetch is implemented.
- `local.properties` is git-ignored (`.gitignore`).

## Webhooks & Callbacks

**Incoming:**
- None. The Ktor server (`server/`) only defines `GET /` (`Application.kt`).

**Outgoing:**
- None detected.

---

*Integration audit: 2026-11-27*
