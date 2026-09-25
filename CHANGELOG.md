# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## 2.0.0

- Rewritten from scratch on Compose Multiplatform; same features and settings.
- Circles are sized in dp, so they look the same on every screen density.
- Every finger is drawn (previously only the first 10).
- Groups show group numbers; Order and Groups labels pick black or white for readable contrast.
- The result overlay fades out faster.
- Group size is 2–10 everywhere.

## [Unreleased]

### Added - Phase 2 & 3: UI Separation, Theming, and Accessibility
- `TouchEventListener` interface for high-level gesture management
- Long-press to reset functionality during result display
- Dynamic theming integration: `KmwaziTheme` now uses the selected `Palette` for its `colorScheme`
- Accessibility support:
  - Meaningful `contentDescription` for all interactive elements
  - Live regions for result announcements and settings updates
  - Click labels for palette selection

### Changed - Phase 2 & 3: UI Separation, Theming, and Accessibility
- Refactored `trackMultiTouch` to use `TouchEventListener` for better separation of concerns
- `MainActivity` and `KmwaziApp` now observe and apply the current palette to the theme
- Improved touch targets across all screens to ensure >= 48dp

### Added - Phase 1: Architecture and Testability
- `RandomProvider` interface for deterministic testing
- `ResultEngine` for pure domain logic computation
- `SettingsRepository` for persisted configuration
- `CountdownController` for testable timer logic
- `ServiceLocator` for manual dependency injection

### Changed - Phase 1: Architecture and Testability
- `TouchViewModel` refactored to use `ResultEngine`, `CountdownController`, and `SettingsRepository`
- Comprehensive unit test coverage for domain and viewmodel logic

### Removed - Phase 2: UI Separation
- Oversized `Screens.kt` (split into individual files in `ui/screens/`)

## Technical Debt Status

Completed Phases 0, 1, and most of Phase 2 and 3:
- [x] Phase 0: Safety net and hygiene
- [x] Phase 1: Architecture and testability
- [x] Phase 2: UI separation and theming
- [x] Phase 3: Accessibility and UX polish (Partial)

## Breaking Changes

None yet. All changes in Phase 0-1 maintain backward compatibility.

## Known Issues

- ktlint reports 30 violations for inline comments in Palette.kt (non-blocking, configured to ignore)
- Build.gradle.kts uses deprecated kotlinOptions DSL (migration to compilerOptions planned)
