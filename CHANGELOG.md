# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added - Phase 0: Safety Net and Hygiene
- ktlint plugin (v12.1.1) for code quality enforcement
- Compose lint checks for better Compose code quality
- Test dependencies:
  - kotlinx-coroutines-test (1.9.0) for testing coroutines
  - Turbine (1.2.0) for testing Flows
- .editorconfig file with ktlint configuration
- CHANGELOG.md to track all changes

### Changed - Phase 0: Safety Net and Hygiene
- Configured ktlint with Android-specific rules
- Enabled lint checks for ComposeUnstableReceiver and ComposeModifierMissing
- Set ktlint to ignore failures temporarily to establish baseline

### Removed - Phase 0: Safety Net and Hygiene
- ExampleUnitTest.kt (boilerplate test)
- ExampleInstrumentedTest.kt (boilerplate test)

### Added - Phase 1: Architecture and Testability
(Work in progress)

### Changed - Phase 1: Architecture and Testability
(Work in progress)

### Removed - Phase 1: Architecture and Testability
(Work in progress)

## Technical Debt Status

This release focuses on **Phases 0-1** from TECHNICAL_DEBT.md:
- [x] Phase 0.1: Code quality tools configured (ktlint, lint)
- [x] Phase 0.2: CHANGELOG.md created
- [x] Phase 0.3: Test infrastructure set up
- [ ] Phase 1.1: RandomProvider interface created
- [ ] Phase 1.2: ResultEngine extracted from SecureRandomUtils
- [ ] Phase 1.3: SettingsRepository interface created
- [ ] Phase 1.4: CountdownController extracted from ViewModel
- [ ] Phase 1.5: Manual dependency injection with ServiceLocator
- [ ] Phase 1.6: TouchViewModel refactored to use injected dependencies
- [ ] Phase 1.8: Comprehensive unit test coverage

## Breaking Changes

None yet. All changes in Phase 0-1 maintain backward compatibility.

## Known Issues

- ktlint reports 30 violations for inline comments in Palette.kt (non-blocking, configured to ignore)
- Build.gradle.kts uses deprecated kotlinOptions DSL (migration to compilerOptions planned)
