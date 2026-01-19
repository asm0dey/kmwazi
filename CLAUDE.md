# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Kmwazi is a Chwazi-inspired Android tablet application for randomly selecting players, forming groups, or defining play order based on multi-touch finger detection. The app detects up to 10 simultaneous touches, waits for stabilization (4 seconds), then animates results.

**Key Technologies:**
- Kotlin + Jetpack Compose
- Android Gradle Plugin 8.12.1
- Minimum SDK 21, Target SDK 36
- MVVM architecture

## Build Commands

```bash
# Build the project
./gradlew build

# Run unit tests
./gradlew test

# Run instrumented tests (requires connected device/emulator)
./gradlew connectedAndroidTest

# Run a specific test class
./gradlew test --tests com.github.asm0dey.kmwazi.ExampleUnitTest

# Clean build
./gradlew clean build

# Install debug APK
./gradlew installDebug

# Build release APK
./gradlew assembleRelease
```

## Architecture

### Core Structure (MVVM + Compose)

- **ui/**: Composables, themes, navigation
  - `screens/Screens.kt`: All screen composables (535 lines - known to be oversized, refactoring planned)
  - `Theme.kt`, `Palette.kt`: Material3 theming and color palettes
  - `navigation/NavGraph.kt`: Navigation setup
- **viewmodel/**: `TouchViewModel` manages state for touch tracking, timer, and result computation
- **domain/**: Business logic
  - `Mode.kt`: Sealed interfaces for 3 modes (ChooseOne, SplitIntoGroups, DefineOrder) and results
  - `SecureRandomUtils.kt`: SecureRandom-based randomization (chooseOne, splitIntoGroups, defineOrder)
- **data/**: `SettingsRepository` using DataStore for palette/timeout persistence

### Touch Detection Flow

1. **Compose PointerInput** (in `TouchScreen`): Tracks up to 10 finger positions via `awaitEachGesture`
2. **Stabilization Timer** (in `TouchViewModel`): Resets on touch count changes; fires after configured timeout (default 3s)
3. **Snapshot & Lock**: When timer expires, captures touch positions and locks input
4. **Result Computation**: Uses `SecureRandomUtils` based on selected mode
5. **Animation**: Mode-specific animations (choose one: highlight, groups: color clusters, order: numbered badges)

### State Management

- `TouchViewModel` is the single source of truth for:
  - Active touches (`activePoints`)
  - Timer state (`remainingMs`, `decisionTimeoutMs`)
  - Input lock status (`inputLocked`)
  - Snapshot of stabilized touches (`snapshot`)
  - Current mode (`mode`)
  - Computed result (`result`)
- UI observes flows via `collectAsState()`

### Randomization

Uses `java.security.SecureRandom` for fairness:
- **ChooseOne**: `nextInt(n)` for uniform selection
- **SplitIntoGroups**: Shuffle then chunk by group size
- **DefineOrder**: Shuffle and assign sequential numbers

## Key Design Decisions

### Multi-touch Handling
- Pointer tracking via `mutableStateMapOf<Long, Offset>` for finger IDs and positions
- Stabilization resets on any touch count change (add/remove)
- Cap at 10 simultaneous touches

### Color Assignment
- 3+ predefined palettes (vibrant, pastel, monochrome) with 10 colors each
- Stable color assignment per finger based on order detected
- WCAG AA contrast compliance

### Persistence
- **Only** user preferences stored (color palette, timeout)
- No touch data or results persisted
- Uses DataStore Preferences

### Offline Operation
- Fully functional offline
- No network permissions
- Only permission: `VIBRATE` for haptic feedback

## Known Technical Debt

**See `TECHNICAL_DEBT.md` for complete tracking with checkboxes.**

Main issues:

1. **Screens.kt is oversized** (535 lines): Mixes UI, gesture handling, state, and drawing logic
   - Planned split: HomeScreen.kt, TouchScreen.kt, SettingsScreen.kt, HelpScreen.kt, ui/draw/FingerCanvas.kt, ui/gestures/MultiTouchTracker.kt

2. **SecureRandomUtils uses static SecureRandom**: Makes testing difficult
   - Plan: Introduce `RandomProvider` interface for dependency injection

3. **Build configuration inconsistencies**: Versions declared both in `libs.versions.toml` and inline in `app/build.gradle.kts`
   - Compose BOM defined inline instead of in version catalog

4. **Tight coupling in TouchScreen**: Gesture handling, drawing, and state updates are intertwined
   - Plan: Extract gesture logic into reusable component with clear callbacks

5. **Theme integration incomplete**: Palettes not fully integrated with Material3 colorScheme
   - Hardcoded `Color.Black` backgrounds instead of Material theme values

Full refactoring roadmap: `docs/plan.md`

## Development Workflow

1. **Adding a new mode**:
   - Add sealed class variant to `domain/Mode.kt`
   - Implement randomization logic in `SecureRandomUtils.kt`
   - Add result type to `domain/Mode.kt` Result interface
   - Update `TouchViewModel.computeResult()`
   - Add UI rendering in `TouchScreen` composable

2. **Adding a new palette**:
   - Add to `Palettes` object in `ui/Palette.kt`
   - Ensure 10 distinct colors with WCAG AA contrast
   - Palette selection handled automatically via settings

3. **Modifying touch stabilization**:
   - Timer logic in `TouchViewModel.restartTimer()`
   - Configured via `setDecisionTimeoutSeconds()` (1-10 seconds)
   - Default: 3 seconds (stored in DataStore)

## Testing Strategy

- **Unit tests**: Domain logic (randomization, grouping edge cases, timer behavior)
- **Instrumented tests**: Touch flow, settings persistence, accessibility
- Test files: `app/src/test/` and `app/src/androidTest/`

## Important Constraints

- **No backwards-compatibility needed**: Clean up unused code completely
- **Accessibility required**: WCAG AA compliance, screen reader support, haptic feedback
- **Performance target**: <100ms touch response on mid-range devices
- **Android 5.0+ support**: minSdk 21, but uses modern Compose
- **Tablet-optimized**: Designed for 7"+ screens with multi-touch

## Documentation References

- `docs/requirements.md`: Full functional and non-functional requirements
- `docs/plan.md`: Implementation roadmap and refactoring plan with checkboxes
