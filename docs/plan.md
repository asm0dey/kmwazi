# Refactoring and Improvement Plan (Checkboxable)

Version: 1.0
Date: 2025-08-21

Executive summary
- Purpose: Improve maintainability, testability, performance, accessibility, and UX while keeping current functionality intact.
- Strategy: Incremental refactors in small, reversible steps with measurable outcomes and test coverage.
- Constraints: No functional regressions; keep package structure and public APIs stable unless explicitly noted.

Guiding principles
- Separation of concerns: UI vs. state vs. domain logic.
- Testability: Pure functions and injectable dependencies.
- Compose best practices: State hoisting, minimal recomposition, Material3 theming.
- Accessibility and performance as first-class concerns.
- Build hygiene and reproducibility.

Audit summary (current state)
- Oversized Screens.kt (535 lines) mixes UI, gesture handling, state, and drawing logic.
- TouchViewModel owns timer and results but UI still handles gesture and reset logic tightly coupled to Canvas.
- SecureRandomUtils uses static SecureRandom making deterministic tests hard.
- Settings: Mixed persistence (DataStore) and in-memory palette repository; no clear interface boundary.
- Theme: Minimal Material3 theme; palettes not integrated with Material color scheme. Activity forces dark theme/black surface.
- Navigation: Reasonable, but contains auto-navigation side-effects in NavHost; could be lifted to a higher layer or guarded.
- Build: Versions declared both in toml and inline; inconsistent versions (e.g., navigation, lifecycle); compose BOM set inline.

Milestone roadmap (phased, safe to batch)
- Phase 0: Safety net and hygiene
  [ ] Add ktlint/spotless and baseline lint configuration for Compose and accessibility checks.
  [ ] Introduce basic unit tests for domain utilities and ViewModel timer logic.
  [ ] Add detekt (optional) with a minimal config for long files and complexity reporting.
  [ ] Establish CHANGELOG.md and CODEOWNERS (optional) for process clarity.
- Phase 1: Architecture and testability
  [ ] Create interfaces for randomness and settings.
      - [ ] RandomProvider with nextInt/shuffle and a SecureRandomProvider implementation.
      - [ ] SettingsRepository becomes an interface; provide DataStoreSettingsRepository implementation.
  [ ] Inject dependencies into TouchViewModel via a simple ServiceLocator or DI (Hilt/Koin later).
  [ ] Move result computation to a pure domain service (ResultEngine) using injected RandomProvider.
  [ ] Add unit tests for ResultEngine: choose one, groups, order (edge cases: empty, groupSize<=0 rejected, >size).
- Phase 2: UI separation and theming
  [ ] Split Screens.kt into smaller files:
      - [ ] HomeScreen.kt, TouchScreen.kt, SettingsScreen.kt, HelpScreen.kt, and a drawing package (e.g., ui/draw/FingerCanvas.kt).
      - [ ] Extract pointer tracking into a reusable pointerInput helper/io class (ui/gestures/MultiTouchTracker.kt) with clear callbacks.
  [ ] Integrate palettes into Material theme.
      - [ ] Extend KmwaziTheme to derive colorScheme from selected palette (limited mapping: primary/secondary/tertiary) and surface/background.
      - [ ] Remove hardcoded black backgrounds in favor of Material colorScheme.
  [ ] Move long-press reset logic out of Canvas block into a higher-level gesture handler with clear states.
- Phase 3: Accessibility and UX polish
  [ ] Ensure semantics/labels for controls and announce results via Compose semantics.
  [ ] Validate contrast for palettes; add a high-contrast fallback.
  [ ] Provide consistent haptic feedback hooks; guard by SDK.
  [ ] Improve on-screen controls: replace ad-hoc DropdownMenu with a structured sheet/dialog for modes + group size.
- Phase 4: Performance and stability
  [ ] Optimize recomposition: use derivedStateOf for computed maps (orderMap/groupsMap) and remember paints.
  [ ] Reduce allocations in Canvas code; reuse AndroidPaint objects.
  [ ] Cap finger colors rotation with stable assignment across sessions if desired (optional).
  [ ] Add simple performance test or macrobenchmark for pointer processing.
- Phase 5: Build and CI
  [ ] Consolidate versions via libs.versions.toml only; remove inline versions or map them into the catalog.
  [ ] Use Compose BOM from versions catalog and align artifacts.
  [ ] Set consistent lifecycle/navigation versions; avoid mixing snapshot-like versions.
  [ ] Add a lightweight CI workflow (e.g., GitHub Actions) to run unit tests and ktlint/detekt.

Detailed checklist by area
Architecture and domain
- [ ] Introduce RandomProvider interface
  - [ ] fun nextInt(bound: Int): Int
  - [ ] fun <T> shuffle(list: List<T>): List<T>
- [ ] Implement SecureRandomProvider using java.security.SecureRandom
- [ ] Refactor SecureRandomUtils into ResultEngine using RandomProvider
- [ ] TouchViewModel depends on ResultEngine and exposes intents: setMode, setTimeout, updateActive, reset
- [ ] Make timer logic pure where possible (extract CountdownController for unit tests)

Data and settings
- [ ] Define SettingsRepository interface with flows and save methods
- [ ] Implement DataStoreSettingsRepository and migrate object to DI-provided instance
- [ ] Unify PaletteRepository with persisted settings; expose palette flow via SettingsRepository only
- [ ] Add data migration if keys/names change (future-proofing)

UI and gestures
- [ ] Extract MultiTouch gesture handling into a function/class with clear callbacks
- [ ] Keep TouchScreen composable free from business logic (render state only)
- [ ] Move number drawing and paint setup into a small helper; remember paint instances
- [ ] Introduce a ResultOverlay composable for animations per mode

Theming
- [ ] Rework KmwaziTheme to accept a Palette and map to Material3 colorScheme
- [ ] Replace hardcoded Surface(Color.Black) with MaterialTheme surface/background
- [ ] Optionally add dynamic color support on Android 12+

Navigation
- [ ] Remove side-effectful auto-navigation from NavHost or guard it via a one-shot event from a ViewModel
- [ ] Centralize routes in a sealed class or @Serializable destinations (optional)

Accessibility
- [ ] Add meaningful contentDescription for all interactive elements
- [ ] Announce results via semantics
- [ ] Ensure touch targets >= 48dp and typography scale friendly

Performance
- [ ] Use derivedStateOf for orderMap/groupsMap computations
- [ ] Limit Canvas work; avoid per-frame object creation
- [ ] Consider snapshot flow throttling to avoid excessive recompositions

Testing
- [ ] Unit tests: ResultEngine, CountdownController, SettingsRepository impl (using TestDataStore)
- [ ] UI tests: mode selection, settings persistence, basic TouchScreen rendering with fake inputs (where feasible)

Build and tooling
- [ ] Align versions via versions catalog; replace hardcoded compose BOM and libraries with catalog references
- [ ] Add ktlint/spotless Gradle plugin; wire to CI
- [ ] Add detekt (optional)

Risk management and rollback
- [ ] Commit in small steps; keep features flag-guarded if needed
- [ ] Maintain behavior parity after each phase; verify with smoke tests

Success metrics
- [ ] Files split: Screens.kt reduced to <200 lines per file
- [ ] Test coverage for domain logic >= 80%
- [ ] No noticeable regressions in touch latency; average frame time stable
- [ ] Lint passes without new warnings; accessibility checks satisfied

Timeline (indicative)
- Phase 0–1: 1–2 days
- Phase 2: 1–2 days
- Phase 3: 1 day
- Phase 4: 0.5–1 day
- Phase 5: 0.5 day

Appendix: mapping to current files
- app/src/main/java/.../viewmodel/TouchViewModel.kt → inject ResultEngine, extract timer helper
- app/src/main/java/.../domain/SecureRandomUtils.kt → replace with ResultEngine
- app/src/main/java/.../ui/screens/Screens.kt → split into multiple files + draw/gestures helpers
- app/src/main/java/.../ui/Theme.kt and Palette.kt → integrate palette into Material colorScheme
- app/src/main/java/.../ui/navigation/NavGraph.kt → remove auto-nav side-effects or handle via VM
- app/build.gradle.kts, gradle/libs.versions.toml → consolidate versions, add plugins

# Implementation Plan for Chwazi-like Android Tablet App

Version: 1.0
Date: 2025-08-21

## 1. Executive Summary
This plan outlines how to implement a Chwazi-inspired multi-touch Android application for tablets (Android 5.0+). The app detects up to 10 simultaneous fingers, waits until touches stabilize for 4 seconds, then computes and animates results for one of three modes: Choose One, Split into Groups, and Define Play Order. The solution prioritizes performance, accessibility, offline operation, and a modern, maintainable codebase using Jetpack Compose.

## 2. Goals and Success Criteria
- Detect and track up to 10 simultaneous touches accurately.
- Implement a robust stabilization algorithm that resets on touch count changes and fires after 4s of stability.
- Provide 3 modes with correct logic and secure randomness using SecureRandom.
- Deliver smooth animations (1–2s, targeting 60 FPS) per mode.
- Offer at least 3 accessible color palettes, selectable via settings.
- Achieve <100ms response to touch add/remove on mid-range devices.
- Ensure accessibility: WCAG AA contrast, screen reader hints, haptics.
- Store only user preferences (color palette) locally; no touch data persistence.
- App fully functional offline.

Rationale: These criteria map directly to functional and non-functional requirements, making acceptance unambiguous and testable.

## 3. Scope and Assumptions
- Scope: Multi-touch detection screen, mode selection UI, per-mode result logic and animations, settings (palette), basic tutorial, preferences storage, accessibility support.
- Out of scope: Networking, advanced analytics, cloud sync, external sensors.
- Assumptions: Device supports ≥10 touch points; users select a mode before touching; tablet-sized layout (≥7").

## 4. Architecture and Modules
- Single-module Android app (app/), minSdk 21, target latest stable SDK.
- MVVM + Jetpack Compose for UI:
  - ui: Composables, themes, navigation.
  - viewmodel: State management for mode selection, touches, timer, animations.
  - domain: Touch stabilization logic, randomization, grouping/order algorithms.
  - data: Preferences repository using SharedPreferences (or DataStore as optional improvement) to store selected palette.

Rationale: MVVM with Compose yields a reactive, testable structure. Separation of domain logic enables unit testing without Android framework.

## 5. Input Handling and Stabilization
- Use Android multi-touch via Compose’s PointerInput and awaitPointerEventScope to track pointer down/move/up with pointerId -> position mapping.
- Maintain TouchState: map of active pointers with position and timestamp; expose as immutable list for UI.
- Stabilization algorithm:
  - When active touch count changes, start/reset a 4-second countdown.
  - If any pointer is added/removed before expiry, reset timer.
  - On expiry, snapshot the current touches (positions and IDs) and lock input until result animation completes.
- Edge cases:
  - Ignore micro-movements during stabilization; only count changes in active pointer set for resets.
  - If count drops to 0, cancel stabilization and return to idle.
  - Cap at 10 touches; ignore additional pointers.

Rationale: Using pointerId tracking avoids false resets due to slight movement. Snapshotting ensures deterministic animation based on stabilized inputs.

## 6. Randomization Strategy
- Use java.security.SecureRandom for all randomness.
- Choose One: uniform random index in [0, n).
- Split into Groups: shuffle list with SecureRandom, then partition by n; remainder forms final smaller group.
- Define Play Order: shuffle list with SecureRandom; assign 1..n.

Rationale: SecureRandom provides fairness per requirement 4.2.

## 7. Modes of Operation: UX and Logic
- Mode selection present on the main screen before touch phase; modes: Choose One, Split into Groups, Define Play Order.
- Split into Groups input: numeric stepper or slider for group size n (1..current touches or 1..10 when idle). Disable start until valid.
- During touch phase, display finger circles at detected positions using current palette.
- After stabilization:
  - Choose One: Highlight chosen finger; dim others; show “Selected!” label near position.
  - Split into Groups: Draw group outlines or cluster halos; distinct colors per group; show group labels (A, B, C or 1, 2, 3).
  - Define Play Order: Place numbered badges next to finger positions; sequentially animate numbers.

Rationale: Keeps interaction minimal and mirrors user expectations from Chwazi-like apps.

## 8. Visual Design and Color Palettes
- Provide at least 3 palettes (10 colors each) with WCAG AA contrast on dark and light backgrounds:
  - Vibrant, Pastel, Monochrome (tints/accents).
- Implement via Compose theme with Palette model {name, colors: List<Color>}.
- Palette selection in Settings; persist choice; default to Vibrant.
- Assign colors deterministically: index by stabilized finger order; in groups mode, use per-group base color with shade variations if needed.

Rationale: Predefined palettes ensure accessible differentiation for up to 10 touches.

## 9. Animation System
- Use Compose animations (animateFloatAsState, AnimatedVisibility, updateTransition).
- Duration: 1–2s depending on mode; 60 FPS target by keeping draw ops simple.
- Choose One: Fade in selected, fade out others; glow/ripple around the winner.
- Split into Groups: Smooth color transitions and optional slight translation toward group centroid; draw outlines after transition.
- Play Order: Scale-in numbers sequentially (100–150ms stagger).
- Lock input during animation; then show reset/action buttons.

Rationale: Compose provides efficient, declarative animations aligned with requirement 4.4.

## 10. Accessibility and Haptics
- Contrast: Validate palette-text/background combinations meet WCAG AA using contrast checks when designing palette.
- Screen reader: Content descriptions for mode buttons; result announcements via TalkBack (announce live region or accessibilityService APIs via Semantics in Compose).
- Haptics: Vibrate on first touch detected and when result is revealed (use VibrationEffect for API 26+, fallback to deprecated vibrate for older).
- Touch target sizes ≥ 48dp; readable typography.

Rationale: Meets 3.4 requirements and improves inclusivity.

## 11. Performance and Optimizations
- Efficient pointer tracking with minimal allocations; reuse data structures.
- Cap draw operations: simple shapes (Canvas drawCircle, drawIntoCanvas only if needed).
- Avoid recomposition storms by hoisting state and using derivedStateOf for computed values.
- Measure: Ensure touch handling under 100ms using simple logs and profiling.

Rationale: Ensures smooth UX on mid-range hardware.

## 12. Data Persistence
- Store selected palette in SharedPreferences (PreferencesDataStore optional upgrade).
- No storage of touch positions or results.
- SettingsRepository wraps storage and exposes Flow/State.

Rationale: Minimal persistence meets privacy and offline constraints.

## 13. Security and Privacy
- No network permissions; no sensitive data captured.
- Use only VIBRATE permission if required.
- Randomness via SecureRandom.

## 14. Offline and Permissions
- Operate fully offline.
- Permissions: android.permission.VIBRATE only.

## 15. UI Navigation and Screens
- Screens:
  - Home/Mode Selection (includes group size control).
  - Touch/Play Screen (live touch view; shows timer indicator and stabilization status).
  - Result State (overlay on Touch Screen; provides Reset button).
  - Settings (palette selection).
  - Help/Tutorial (brief guide with illustrations/text).
- Navigation: Simple single-activity with Compose Navigation.

Rationale: Keeps app simple yet clear.

## 16. Testing Strategy
- Unit tests:
  - Stabilization timer logic (resets on add/remove; expires correctly).
  - Randomization correctness (uniform selection, stable shuffles using seeded SecureRandom in tests).
  - Grouping edge cases (not divisible; n==1; n==touchCount; n>touchCount handling in UI).
- UI tests (Compose):
  - Mode selection flow; settings update persists.
  - Accessibility labels exist; TalkBack announcements triggered (where feasible via semantics assertions).
- Performance checks:
  - Basic benchmark for touch handling latency and animation frame stability.

Rationale: Automated tests reduce regressions and verify requirements.

## 17. Build, Tooling, and CI
- Gradle with Kotlin DSL (already present).
- Enable Jetpack Compose in module gradle; set minSdk 21.
- Lint checks for accessibility and performance.
- Optional GitHub Actions/CI to run unit/UI tests.

## 18. Risks and Mitigations
- Risk: Compose pointer APIs behavior differences across OEMs.
  - Mitigation: Test on multiple emulators/devices; add fallbacks.
- Risk: Performance hiccups with many animations.
  - Mitigation: Keep animations simple; profile early.
- Risk: WCAG contrast violations with custom palettes.
  - Mitigation: Pre-validate palettes; provide a high-contrast option.

## 19. Implementation Roadmap and Milestones
1. Project setup: Compose, theming, navigation, settings skeleton (1–2 days). ✓ Completed on 2025-08-20
2. Touch tracking basics with on-screen circles; cap at 10 (1–2 days). ✓ Completed on 2025-08-20
3. Stabilization timer logic with visual countdown (1 day). ✓ Completed on 2025-08-20
4. SecureRandom utilities and algorithms for three modes (1 day). ✓ Completed on 2025-08-20
5. Mode UIs and result computation pipeline (2–3 days). In progress: basic per-mode visuals (group colors, order numbers) added on 2025-08-20.
6. Animations per mode (2 days).
7. Color palettes and settings persistence (1 day).
8. Accessibility (semantics, haptics, contrast validation) (1–2 days).
9. Help screen and polish (1 day).
10. Testing (unit + basic UI), performance profiling (2 days).
11. Packaging, documentation, and user guide (1 day).

## 20. Acceptance Criteria Traceability
- FR 2.1: Multi-touch detection up to 10 with stabilization timer implemented and tested.
- FR 2.2: Three modes functional with secure randomness and correct UI displays.
- FR 2.3: ≥3 palettes selectable; AA contrast verified; up to 10 distinct colors.
- FR 2.4: Mode-specific animations 1–2s, smooth on mid-range hardware.
- NFR 3.x: Tablet optimization, performance <100ms, usability and tutorial, accessibility support.
- TR 4.x: MultiTouch API via Compose, SecureRandom, Compose UI/animations, SharedPreferences for prefs.
- Constraints: Offline, minimal permissions, no sensitive data stored.

## 21. Data Models (Draft)
- TouchPoint: id: Long, x: Float, y: Float
- TouchSnapshot: points: List<TouchPoint>, timestamp: Long
- Mode: ChooseOne | SplitIntoGroups(groupSize: Int) | DefineOrder
- Palette: name: String, colors: List<Color>

## 22. Key Pseudocode
Stabilization timer:
```
onActivePointersChanged() {
  if (activeCount == 0) cancelTimer()
  else restartTimer(4s)
}

timer.onExpire {
  snapshot = activePointers.copy()
  lockInput()
  computeAndAnimateResult(snapshot)
}
```

Grouping:
```
fun groups(points, n): List<List<TouchPoint>> {
  val shuffled = points.shuffled(secureRandom)
  return shuffled.chunked(n)
}
```

Order:
```
val order = points.shuffled(secureRandom).mapIndexed { i, p -> p to (i+1) }
```

Choose one:
```
val index = secureRandom.nextInt(points.size)
val winner = points[index]
```

## 23. Rationale Summary
- Compose + MVVM improves developer velocity and testability.
- PointerInput APIs provide fine-grained multi-touch handling.
- SecureRandom ensures fairness and compliance.
- Predefined accessible palettes meet WCAG and usability constraints.
- Minimal persistence protects privacy and simplifies offline operation.

## 24. Documentation and User Guide
- Include an in-app help screen and a README/User Guide describing modes, finger placement, and interpreting results.
- Developer docs: Document touch tracking, stabilization, and mode algorithms in code comments and a short doc in /docs.
