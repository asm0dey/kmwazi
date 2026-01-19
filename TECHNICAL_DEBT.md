# Technical Debt Tracking

This file tracks technical debt elimination progress for the Kmwazi project.
Based on the detailed refactoring plan in `docs/plan.md`.

## Quick Status Overview

- [ ] Phase 0: Safety net and hygiene
- [ ] Phase 1: Architecture and testability
- [ ] Phase 2: UI separation and theming
- [ ] Phase 3: Accessibility and UX polish
- [ ] Phase 4: Performance and stability
- [ ] Phase 5: Build and CI

---

## Phase 0: Safety Net and Hygiene

### Code Quality Tools
- [ ] Add ktlint/spotless with baseline configuration
- [ ] Configure Compose-specific lint checks
- [ ] Add accessibility lint checks
- [ ] Introduce detekt (optional) for long files and complexity reporting

### Testing Foundation
- [ ] Add basic unit tests for domain utilities (SecureRandomUtils)
- [ ] Add basic unit tests for ViewModel timer logic
- [ ] Set up test infrastructure for deterministic random testing

### Process
- [ ] Establish CHANGELOG.md for tracking changes
- [ ] Add CODEOWNERS file (optional)

---

## Phase 1: Architecture and Testability

### Dependency Injection Setup
- [ ] Create `RandomProvider` interface
  - [ ] Define `fun nextInt(bound: Int): Int`
  - [ ] Define `fun <T> shuffle(list: List<T>): List<T>`
- [ ] Implement `SecureRandomProvider` using `java.security.SecureRandom`
- [ ] Create `SettingsRepository` interface
- [ ] Implement `DataStoreSettingsRepository`
- [ ] Set up simple ServiceLocator or DI container (Hilt/Koin for later)

### Domain Logic Extraction
- [ ] Create pure `ResultEngine` domain service
- [ ] Inject `RandomProvider` into `ResultEngine`
- [ ] Move result computation from ViewModel to `ResultEngine`
- [ ] Refactor `SecureRandomUtils` into `ResultEngine`

### ViewModel Refactoring
- [ ] Inject `ResultEngine` into `TouchViewModel`
- [ ] Extract timer logic into `CountdownController` for testability
- [ ] Make timer logic pure and unit-testable

### Unit Tests
- [ ] Test `ResultEngine.chooseOne()` with edge cases
- [ ] Test `ResultEngine.splitIntoGroups()` with edge cases:
  - [ ] Empty list handling
  - [ ] groupSize <= 0 rejection
  - [ ] groupSize > list size handling
- [ ] Test `ResultEngine.defineOrder()` with various sizes
- [ ] Test `CountdownController` timer behavior
- [ ] Test `DataStoreSettingsRepository` (using TestDataStore)

---

## Phase 2: UI Separation and Theming

### File Splitting (Break up Screens.kt - 535 lines)
- [ ] Extract `HomeScreen.kt` from `Screens.kt`
- [ ] Extract `TouchScreen.kt` from `Screens.kt`
- [ ] Extract `SettingsScreen.kt` from `Screens.kt`
- [ ] Extract `HelpScreen.kt` from `Screens.kt`
- [ ] Create `ui/draw/FingerCanvas.kt` for drawing logic
- [ ] Create `ui/gestures/MultiTouchTracker.kt` for pointer tracking
- [ ] Delete original oversized `Screens.kt` file

### Gesture Handling Extraction
- [ ] Extract pointer tracking into reusable `pointerInput` helper
- [ ] Create clear callbacks interface for touch events
- [ ] Move long-press reset logic out of Canvas block
- [ ] Implement higher-level gesture handler with clear states

### Theming Integration
- [ ] Extend `KmwaziTheme` to accept Palette parameter
- [ ] Derive Material3 `colorScheme` from selected palette
  - [ ] Map palette colors to primary/secondary/tertiary
  - [ ] Set surface/background colors from palette
- [ ] Remove hardcoded `Color.Black` backgrounds
- [ ] Replace all hardcoded colors with `MaterialTheme.colorScheme` references
- [ ] Test theme with all three palettes

### Drawing Optimization
- [ ] Extract number drawing into helper function
- [ ] Remember paint instances to avoid recreation
- [ ] Move paint setup out of draw loop
- [ ] Create `ResultOverlay` composable for animations

---

## Phase 3: Accessibility and UX Polish

### Semantics and Screen Readers
- [ ] Add meaningful `contentDescription` for all interactive elements
- [ ] Add semantics for mode selection controls
- [ ] Announce results via Compose semantics (live region)
- [ ] Test with TalkBack enabled

### Contrast and Visual Accessibility
- [ ] Validate contrast for all palettes (WCAG AA compliance)
- [ ] Add high-contrast fallback palette
- [ ] Ensure touch targets >= 48dp
- [ ] Verify typography scales appropriately

### Haptic Feedback
- [ ] Provide consistent haptic feedback hooks
- [ ] Guard haptic calls by SDK version
- [ ] Add haptic feedback on first touch detected
- [ ] Add haptic feedback when result is revealed

### UI Controls Improvement
- [ ] Replace ad-hoc DropdownMenu with structured design
- [ ] Create proper sheet/dialog for mode selection
- [ ] Create proper input for group size selection
- [ ] Improve reset button UX

---

## Phase 4: Performance and Stability

### Recomposition Optimization
- [ ] Use `derivedStateOf` for `orderMap` computation
- [ ] Use `derivedStateOf` for `groupsMap` computation
- [ ] Audit and minimize unnecessary recompositions
- [ ] Use `remember` for paint objects

### Canvas Performance
- [ ] Reduce allocations in Canvas code
- [ ] Reuse `AndroidPaint` objects
- [ ] Optimize drawing operations
- [ ] Profile Canvas performance on mid-range devices

### Touch Processing
- [ ] Cap finger colors rotation with stable assignment
- [ ] Optimize pointer event processing
- [ ] Consider snapshot flow throttling to avoid excessive recompositions
- [ ] Verify <100ms touch response time

### Performance Testing
- [ ] Add simple performance test for pointer processing
- [ ] Add macrobenchmark for touch flow (optional)
- [ ] Profile frame time during animations

---

## Phase 5: Build and CI

### Version Catalog Consolidation
- [ ] Move all inline versions to `libs.versions.toml`
- [ ] Add Compose BOM to version catalog
- [ ] Remove hardcoded versions from `app/build.gradle.kts`
- [ ] Align all Compose artifact versions with BOM
- [ ] Set consistent lifecycle versions
- [ ] Set consistent navigation versions
- [ ] Remove version conflicts and mixing

### CI Setup
- [ ] Create GitHub Actions workflow for CI
- [ ] Configure workflow to run unit tests
- [ ] Configure workflow to run ktlint/detekt
- [ ] Add build status badge to README (optional)
- [ ] Configure workflow to build APK
- [ ] Add Android instrumented tests to CI (optional)

---

## Data and Settings Unification

### Repository Pattern
- [ ] Unify `PaletteRepository` with persisted settings
- [ ] Expose palette flow via `SettingsRepository` only
- [ ] Remove duplicate state management
- [ ] Add data migration support for future key changes

---

## Navigation Cleanup

### Side-effect Management
- [ ] Remove side-effectful auto-navigation from `NavHost`
- [ ] Guard auto-navigation via one-shot event from ViewModel
- [ ] Centralize routes in sealed class (optional)
- [ ] Make routes `@Serializable` destinations (optional)

---

## Success Metrics

Track these metrics after completing each phase:

- [ ] Files split: `Screens.kt` reduced to <200 lines per file
- [ ] Test coverage for domain logic >= 80%
- [ ] No noticeable regressions in touch latency
- [ ] Average frame time remains stable
- [ ] Lint passes without new warnings
- [ ] Accessibility checks satisfied
- [ ] Zero hardcoded versions in build files
- [ ] CI pipeline green

---

## Risk Management

### Rollback Strategy
- [ ] Commit changes in small, atomic steps
- [ ] Keep features flag-guarded if needed during transition
- [ ] Maintain behavior parity after each phase
- [ ] Run smoke tests after each major change

### Verification Checklist (Run after each phase)
- [ ] All existing tests pass
- [ ] Touch detection still works correctly
- [ ] All three modes produce correct results
- [ ] Animations run smoothly
- [ ] Settings persist correctly
- [ ] No performance regressions

---

## Notes

**Timeline (Indicative):**
- Phase 0–1: 1–2 days
- Phase 2: 1–2 days
- Phase 3: 1 day
- Phase 4: 0.5–1 day
- Phase 5: 0.5 day

**File Mapping Reference:**
- `app/src/main/java/.../viewmodel/TouchViewModel.kt` → inject ResultEngine, extract timer helper
- `app/src/main/java/.../domain/SecureRandomUtils.kt` → replace with ResultEngine
- `app/src/main/java/.../ui/screens/Screens.kt` → split into multiple files + draw/gestures helpers
- `app/src/main/java/.../ui/Theme.kt` and `Palette.kt` → integrate palette into Material colorScheme
- `app/src/main/java/.../ui/navigation/NavGraph.kt` → remove auto-nav side-effects or handle via VM
- `app/build.gradle.kts`, `gradle/libs.versions.toml` → consolidate versions, add plugins
