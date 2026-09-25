# Kmwazi rewrite — design

Date: 2026-09-25 · Branch: `rewrite-kmp` · Status: approved in brainstorming, pending spec review

## Intent

Rewrite Kmwazi from scratch on the same stack (Kotlin + Compose) with the same user experience,
fixing known bugs, so the codebase is small, its rules are testable without a device, and its
architecture is enforced by tests.

Pain points this must fix (all four confirmed by the author):

1. `TouchScreen` is a 398-line god-composable mixing touch state, colors, mode sheet and persistence.
2. Ceremony: `ServiceLocator`, `TouchEventListener`, `CountdownController`, `RandomProvider`, nav routes.
3. Build/tooling cruft: refreshVersions next to a version catalog, ktlint with `ignoreFailures`, iikit/tessl artifacts.
4. Weak tests: round rules are tangled with Compose and coroutines.

## Constraints

- Ships as an in-place update: `applicationId = com.github.asm0dey.kmwazi`, same Play listing.
- Users keep their settings: same DataStore file (`settings`) and keys, no migration.
- Same UX as 1.3.0 except the fixes and amendments listed below.
- Keep: fastlane (beta/deploy lanes), GPL-3.0 license headers (`HEADER`), ktlint (now failing the build).
- Strings: en, de, ru copied unchanged.

## Success criteria

- Every flow in the README works as in 1.3.0 (modulo listed changes).
- Round rules, dealing, settings parsing, label contrast and architecture rules are covered by JVM tests.
- Multi-touch behaviour of the Touch screen is covered by automated JVM UI tests.
- `./gradlew check` fails on ktlint, license-header, lint, test or ArchUnit violations.
- Manual upgrade from 1.3.0 keeps palette, mode/group size and timeout.

## Platforms

Compose Multiplatform with two targets:

- **Android** — the shipped app.
- **Desktop (JVM)** — development and test target only, never released. Hosts all automated tests
  (including multi-touch UI tests via `runComposeUiTest`) and gives a fast `./gradlew :desktopApp:run` preview.

Rejected: web (Wasm) target — Compose for web is Beta and multi-touch in mobile browsers is unverified;
testing in a desktop browser can't exercise multi-touch anyway. Rejected: Android-only — loses JVM UI tests.

## Module layout

AGP 9 requires Android entry points in a module separate from KMP code.

```
settings.gradle.kts        include(":shared", ":app", ":desktopApp")

shared/                    kotlin("multiplatform") + androidMultiplatformLibrary + jvm("desktop") + Compose MP
  src/commonMain/kotlin/com/github/asm0dey/kmwazi/
    round/Round.kt         Mode, Result, Point, Finger, RoundState, Event, reduce()
    round/Deal.kt          chooseOne / groups / order over kotlin.random.Random
    Settings.kt            multiplatform DataStore wrapper, Prefs flow + setters
    Palettes.kt            the 4 palettes (values copied from 1.3.0)
    RoundViewModel.kt      holds RoundState, forwards events, runs the timer
    App.kt                 screen enum + BackHandler; wires screens to VM and Settings
    ui/TouchScreen.kt      layout: canvas, mode button, reset + close buttons
    ui/ModeSheet.kt        bottom sheet with group-size stepper
    ui/FingerCanvas.kt     circles, labels, result overlay
    ui/Pointers.kt         Modifier.multiTouch(onChange: (Map<Long, Point>) -> Unit)
    ui/Home.kt, ui/SettingsScreen.kt, ui/Help.kt
  src/commonMain/composeResources/
    values/strings.xml, values-de/strings.xml, values-ru/strings.xml
    drawable/              4 icons as vector XML: close, play, settings, help
  src/desktopTest/kotlin/…  all tests (Kotest on JUnit Platform)

app/                       com.android.application; MainActivity only
desktopApp/                main() { application { Window { App(...) } } }
```

Wiring: each entry point constructs `SecureRandom().asKotlinRandom()` and a DataStore file path and
passes them into `App(random, settingsPath)`. `App` creates `Settings` and the ViewModel
(`viewModel { RoundViewModel(...) }`). No DI container, no expect/actual.

Removed relative to 1.3.0: `ServiceLocator`/`di`, `TouchEventListener`, `MultiTouchTracker`,
`CountdownController`, `RandomProvider`/`SecureRandomProvider`/`ResultEngine` (folded into `Deal`),
`NavGraph`/`Screen` routes, navigation-compose, the separate `ResultOverlay` file.

## Round rules (`round/Round.kt`)

```kotlin
data class Point(val x: Float, val y: Float)
data class Finger(val pos: Point, val colorIndex: Int)

sealed interface Mode { ChooseOne; Groups(size: Int /* 2..10 */); Order }
sealed interface Result { One(winner: Long); Groups(groups: List<List<Long>>); Order(order: List<Long>) }
data class Outcome(val result: Result, val snapshot: Map<Long, Finger>)

data class RoundState(
    val mode: Mode,
    val fingers: Map<Long, Finger>,
    val nextColor: Int,
    val armed: Int,
    val outcome: Outcome?,        // non-null = locked
)

sealed interface Event {
    FingersChanged(points: Map<Long, Point>); Expired(armed: Int); Reset; ModeChanged(mode: Mode)
}

fun reduce(s: RoundState, e: Event, deal: Deal): RoundState
```

Rules:

1. `FingersChanged` with a different, non-empty id set → `armed++` (restart countdown).
   Position-only changes update positions without re-arming.
2. `FingersChanged` with an empty set while not locked → countdown cancelled (`armed++` so any
   in-flight expiry is stale), `nextColor = 0`.
3. `Expired(n)` with `n == armed`, not locked, fingers present → deal by mode, set `outcome`
   (snapshot of current fingers). Any other `Expired` is ignored.
4. While locked: finger changes update nothing visible until the finger set becomes empty.
   The next non-empty `FingersChanged` after that → reset, then apply rule 1.
5. `Reset` or `ModeChanged` → clear fingers, outcome, `nextColor = 0`, `armed++`; `ModeChanged` sets mode.
6. A new finger gets `colorIndex = nextColor`, then `nextColor++`. Rendering wraps by palette size.

Dealing (`round/Deal.kt`, constructed with a `kotlin.random.Random`):

- `chooseOne(ids)` — uniform pick.
- `groups(ids, size)` — shuffle, `chunked(size)`; last group may be smaller.
- `order(ids)` — shuffle.

Timer (in `RoundViewModel`):

```kotlin
state.map { it.armed }.distinctUntilChanged()
    .collectLatest { n -> delay(timeoutMs); send(Event.Expired(n)) }
```

The timeout is read from `Settings.prefs`.

## UI and rendering

Unchanged from 1.3.0:

- Dark theme always; status bar hidden, transient on swipe (done once in `MainActivity`).
- Home: title + Start / Settings / Help buttons.
- Settings: palette dropdown with colour-stripe previews; timeout stepper 1–10 s with live-region announcement.
- Help: text screen.
- ✕ bottom-right on every non-home screen. Back → Home; back on Home exits. Esc on desktop behaves as back.
- Touch screen:
  - Mode button top-left opens a bottom sheet. Choose one / Order close the sheet;
    Groups keeps it open and shows the −/+ stepper (2–10). Any mode or size change resets the round.
  - Reset button next to ✕ while locked.
  - Haptic on finger down and on result (no-op on desktop).
  - Circles pulse 1.0→1.2× over 1 s, reversing.
  - Before result: palette colour per finger; gray in Groups mode.
  - Choose One result: winner keeps its colour, others dark gray.
  - Order result: circles keep their colours and show their position number.
  - Groups result: circle colour = palette[group index].
  - Live-region announcement of the result.

Changes:

- **Circle radius** `40.dp` (was `110f` raw pixels, so size varied with screen density).
- **All fingers drawn** (1.3.0 drew only the first 10 while counting all of them).
- **Labels:** Order shows position, Groups shows group number (new). Label colour is black or white,
  whichever has the higher WCAG contrast ratio against the circle (`Color.luminance()`).
- **Result overlay:** Choose One / Order grow a half-transparent circle from the winner / first
  finger over 800 ms; Groups sweeps top-down over 800 ms; then the overlay fades out over 300 ms
  with no hold (was: 1000 ms hold, abrupt hide).
- **Long-press reset removed** — it was unreachable (rule 4 always unlocked the round first).
- **Unused `remainingMs` countdown stream removed.**
- Order-number text via `rememberTextMeasurer` + `drawText` (was `android.graphics.Paint`).
- `Modifier.multiTouch` reports the full finger map on each pointer event and ignores
  pointers already consumed by child controls (buttons).

## Settings (`Settings.kt`)

- Multiplatform DataStore, `PreferenceDataStoreFactory.createWithPath`.
  - Android path: `context.filesDir/datastore/settings.preferences_pb` — the file 1.3.0's
    `preferencesDataStore(name = "settings")` writes.
  - Desktop path: `~/.kmwazi/settings.preferences_pb`.
- Keys (unchanged): `mode` (string), `group_size` (int), `palette_name` (string), `decision_timeout_sec` (int).
- Mode values (unchanged): `"ChooseOne"`, `"DefineOrder"`, `"groups"` (+ `group_size`).
- API: `val prefs: Flow<Prefs>` with `Prefs(palette, mode, timeoutSec)`; `suspend fun setPalette/setMode/setTimeout`.
- Reads are tolerant:

  | Stored | Read as |
  |---|---|
  | missing | Vibrant, ChooseOne, 3 s, group size 2 |
  | unknown palette id | Vibrant |
  | unknown mode string | ChooseOne |
  | group size outside 2–10 | clamped (1.3.0 could store 1) |
  | timeout outside 1–10 | clamped |

- Switching away from Groups keeps `group_size` stored.

## Testing

Kotest (`kotest-runner-junit5`, `kotest-assertions-core`, `kotest-property`) on the JUnit Platform,
all in `shared/src/desktopTest`:

- `RoundTest` (`FunSpec`) — one test per rule 1–6, plus stale-expiry and relock cases.
- `DealTest` — property tests over arbitrary id sets:
  groups partition all ids exactly once and all but the last group are full;
  order is a permutation; chooseOne returns a member.
- `SettingsTest` — real DataStore on a temp file: round-trip, exact 1.3.0 values, every clamp/default row.
- `ContrastTest` — every colour in every palette gets a label colour with contrast ≥ 4.5:1.
- `TouchScreenTest` — `runComposeUiTest` + `performTouchInput` with multiple pointers:
  N fingers → N circles; finger set change re-arms; result shows labels; lift-all then touch starts a new round.
- `ArchitectureTest` — ArchUnit core API (`ClassFileImporter` + `rule.check`) inside a Kotest spec:
  1. `..round..` depends only on Kotlin stdlib (no `androidx..`, `kotlinx.coroutines..`, other kmwazi packages).
  2. `..ui..` does not depend on `Settings`, `androidx.datastore..` or `RoundViewModel`.
  3. Nothing in `shared` references `java.security..`.
- `RoundViewModelTest` — `kotlinx-coroutines-test` virtual time: `Expired` fires after the configured timeout,
  and a re-arm before the timeout fires only once.

Manual checks live in `RELEASING.md`: real-finger gestures on a phone
(5+ fingers, lift/re-add, buttons don't register as fingers, reset, mode changes) and the 1.3.0 upgrade check
(set Pastel / Groups of 4 / 7 s on 1.3.0, install over it, confirm all three survive).

## Build and tooling

- Single version source: `gradle/libs.versions.toml`. Remove refreshVersions plugin, `versions.properties`,
  and its two lines in `lint.xml`.
- Add: Kotlin Multiplatform, Compose Multiplatform, `com.android.kotlin.multiplatform.library`,
  Kotest, ArchUnit, multiplatform DataStore, multiplatform lifecycle-viewmodel, kotlinx-coroutines-test,
  Compose UI test.
- Drop: navigation-compose, Turbine, JUnit 4, espresso, androidx-junit, `material` (Views),
  `adaptive`, material icons core/extended.
- `./gradlew check` runs: ktlint on all modules (`ignoreFailures = false`), license-header check over
  `shared/src`, `app/src`, `desktopApp/src` using `HEADER`, Android lint on `:app` (accessibility issues
  stay errors), and `desktopTest`.
- fastlane: `test` lane runs `gradle(task: "check")`; `beta` and `deploy` unchanged (module still `:app`).
- CI (`.github/workflows/android.yml`): unchanged apart from following the fastlane lane.
- Renovate (`renovate.json`): `config:recommended` + grouped non-major `packageRule`
  (minor/patch/digest/pin/bump) + `vulnerabilityAlerts`, per the author's cross-project precedent.
- Version: `2.0.0`, versionCode 4.

To verify during planning: the Kotest version compatible with the project's Kotlin/AGP versions
(only the desktop JVM target runs tests, so Android-plugin compatibility is not required).

## Repository cleanup

Remove every intent-integrity-kit and tessl artifact:

- `CONSTITUTION.md`, `TECHNICAL_DEBT.md`, `docs/plan.md`, `docs/requirements.md`
- `tessl.json`, `.tessl/`, `.mcp.json`
- iikit skill links in `.claude/skills`, `.codex/skills`, `.gemini/skills`, and untracked `.agents/skills`, `.cursor/skills`
- `AGENTS.md` and `CLAUDE.md` (they only include tessl rules)

Keep: `docs/superpowers/`, `.superpowers/`, `.claude/settings.local.json`, `.codex/config.toml`,
`.gemini/settings.json`, README (updated for new layout and desktop run), CHANGELOG (2.0.0 entry), LICENSE, HEADER.

Add: `RELEASING.md` (build, manual checks, `bundle exec fastlane deploy`).

## Out of scope

Web target, iOS, new features, redesign of screens, CI restructuring beyond the fastlane lane change.
